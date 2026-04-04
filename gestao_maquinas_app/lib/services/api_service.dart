// lib/services/api_service.dart

import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http_parser/http_parser.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/registro_diario.dart';

class ApiService {
  // --- PADRÃO SINGLETON: GARANTE QUE SÓ EXISTE UM VIGIA DE REDE NO APP ---
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;

  final Dio _dio;
  final _storage = const FlutterSecureStorage();
  bool _isSyncing = false;
  Timer? _debounceTimer;

  ApiService._internal()
    : _dio = Dio(
        BaseOptions(
          baseUrl: 'https://realizzacontrole.kodarsoftwares.com.br/api/',
          //baseUrl: 'http://192.168.1.10:8082/api/',
          connectTimeout: const Duration(seconds: 5),
          receiveTimeout: const Duration(seconds: 5),
        ),
      ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _storage.read(key: 'jwt_token');
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },
      ),
    );
    _initializeConnectivityListener();
  }

  void _initializeConnectivityListener() {
    Connectivity().onConnectivityChanged.listen((
      List<ConnectivityResult> results,
    ) {
      if (results.any((result) => result != ConnectivityResult.none)) {
        _debounceTimer?.cancel();
        _debounceTimer = Timer(const Duration(seconds: 3), () {
          print('🌐 [CONECTIVIDADE] Internet estável! Sincronizando...');
          _syncOfflineData();
        });
      }
    });
  }

  bool _isNetworkError(DioException e) {
    return e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.connectionError ||
        e.message?.toLowerCase().contains('socket') == true ||
        e.error.toString().toLowerCase().contains('socket');
  }

  Future<void> _syncOfflineData() async {
    if (_isSyncing) return;
    _isSyncing = true;

    try {
      final prefs = await SharedPreferences.getInstance();
      List<String> queue = prefs.getStringList('offline_queue') ?? [];
      if (queue.isEmpty) return;

      List<String> remainingQueue = [];
      for (String item in queue) {
        Map<String, dynamic> task = jsonDecode(item);
        try {
          if (task['status'] == 'READY_TO_SYNC') {
            FormData formData = FormData.fromMap({
              'registro': MultipartFile.fromString(
                jsonEncode(task['registroData']),
                contentType: MediaType.parse('application/json'),
              ),
              'anexoInicio': MultipartFile.fromBytes(
                base64Decode(task['fotoInicioBase64']),
                filename: task['fotoInicioName'],
              ),
              'anexoFinal': MultipartFile.fromBytes(
                base64Decode(task['fotoFinalBase64']),
                filename: task['fotoFinalName'],
              ),
            });
            await _dio.post('/registros', data: formData);
          } else if (task['status'] == 'READY_TO_SYNC_UPDATE') {
            int realId = task['real_id'];
            await _dio.put('/registros/$realId', data: task['registroData']);
            FormData formData = FormData.fromMap({
              'anexoFinal': MultipartFile.fromBytes(
                base64Decode(task['fotoFinalBase64']),
                filename: task['fotoFinalName'],
              ),
            });
            await _dio.post('/registros/$realId/anexo-final', data: formData);
          } else {
            remainingQueue.add(item);
          }
        } catch (e) {
          remainingQueue.add(item);
        }
      }
      await prefs.setStringList('offline_queue', remainingQueue);
    } finally {
      _isSyncing = false;
    }
  }

  Future<Map<String, dynamic>> login(String cpf) async {
    final unmaskedCpf = cpf.replaceAll(RegExp(r'[.-]'), '');
    final response = await _dio.post(
      '/operadores/login',
      data: {'cpf': unmaskedCpf},
    );
    return response.data;
  }

  Future<List<RegistroDiario>> getRegistrosPorOperador(int operadorId) async {
    List<RegistroDiario> registros = [];

    // IMPORTANTE: Dispara o sync em background SEM 'await' para não travar a tela
    _syncOfflineData();

    try {
      final response = await _dio.get('/registros/operador/$operadorId');
      if (response.statusCode == 200) {
        List<dynamic> data = response.data;
        registros = data.map((json) => RegistroDiario.fromJson(json)).toList();
      }
    } catch (e) {
      print('⚠️ Offline: carregando dados locais.');
    }

    final prefs = await SharedPreferences.getInstance();
    List<String> queue = prefs.getStringList('offline_queue') ?? [];
    for (String item in queue) {
      Map<String, dynamic> task = jsonDecode(item);
      if (task['status'] == 'OPEN' &&
          task['registroData']['operadorId'] == operadorId) {
        registros.add(
          RegistroDiario.fromJson({
            'id': task['id_local'],
            'data': task['registroData']['data'],
            'horimetroInicial': task['registroData']['horimetroInicial'],
            'horimetroFinal': null,
          }),
        );
      }
    }
    return registros;
  }

  Future<void> createRegistro(
    Map<String, dynamic> reg,
    Uint8List foto,
    String nome,
  ) async {
    try {
      FormData f = FormData.fromMap({
        'registro': MultipartFile.fromString(
          jsonEncode(reg),
          contentType: MediaType.parse('application/json'),
        ),
        'anexoInicio': MultipartFile.fromBytes(foto, filename: nome),
      });
      await _dio.post('/registros', data: f);
    } on DioException catch (e) {
      if (_isNetworkError(e)) {
        final prefs = await SharedPreferences.getInstance();
        List<String> q = prefs.getStringList('offline_queue') ?? [];
        q.add(
          jsonEncode({
            'id_local': DateTime.now().millisecondsSinceEpoch,
            'status': 'OPEN',
            'registroData': reg,
            'fotoInicioBase64': base64Encode(foto),
            'fotoInicioName': nome,
          }),
        );
        await prefs.setStringList('offline_queue', q);
        return;
      }
      throw 'Erro ao enviar registro.';
    }
  }

  Future<void> updateRegistro(int id, Map<String, dynamic> reg) async {
    if (id > 1000000000) {
      // Registro que já nasceu offline
      final prefs = await SharedPreferences.getInstance();
      List<String> q = prefs.getStringList('offline_queue') ?? [];
      for (int i = 0; i < q.length; i++) {
        Map<String, dynamic> task = jsonDecode(q[i]);
        if (task['id_local'] == id) {
          task['registroData'].addAll(reg);
          q[i] = jsonEncode(task);
          await prefs.setStringList('offline_queue', q);
          return;
        }
      }
      return;
    }

    try {
      await _dio.put('/registros/$id', data: reg);
    } on DioException catch (e) {
      if (_isNetworkError(e)) {
        final prefs = await SharedPreferences.getInstance();
        List<String> q = prefs.getStringList('offline_queue') ?? [];
        // Cria tarefa de fechamento para registro que começou online
        q.add(
          jsonEncode({
            'id_local': DateTime.now().millisecondsSinceEpoch,
            'status': 'UPDATE_ONLY',
            'real_id': id,
            'registroData': reg,
          }),
        );
        await prefs.setStringList('offline_queue', q);
        return;
      }
      throw 'Erro ao atualizar registro.';
    }
  }

  Future<void> uploadAnexoFinal(int id, Uint8List foto, String nome) async {
    if (id > 1000000000) {
      final prefs = await SharedPreferences.getInstance();
      List<String> q = prefs.getStringList('offline_queue') ?? [];
      for (int i = 0; i < q.length; i++) {
        Map<String, dynamic> task = jsonDecode(q[i]);
        if (task['id_local'] == id) {
          task['fotoFinalBase64'] = base64Encode(foto);
          task['fotoFinalName'] = nome;
          task['status'] = 'READY_TO_SYNC';
          q[i] = jsonEncode(task);
          await prefs.setStringList('offline_queue', q);
          return;
        }
      }
      return;
    }

    try {
      FormData f = FormData.fromMap({
        'anexoFinal': MultipartFile.fromBytes(foto, filename: nome),
      });
      await _dio.post('/registros/$id/anexo-final', data: f);
    } on DioException catch (e) {
      if (_isNetworkError(e)) {
        final prefs = await SharedPreferences.getInstance();
        List<String> q = prefs.getStringList('offline_queue') ?? [];
        // Procura a tarefa UPDATE_ONLY criada segundos antes pelo updateRegistro
        bool found = false;
        for (int i = 0; i < q.length; i++) {
          Map<String, dynamic> task = jsonDecode(q[i]);
          if (task['status'] == 'UPDATE_ONLY' && task['real_id'] == id) {
            task['fotoFinalBase64'] = base64Encode(foto);
            task['fotoFinalName'] = nome;
            task['status'] = 'READY_TO_SYNC_UPDATE';
            q[i] = jsonEncode(task);
            found = true;
            break;
          }
        }
        // Se não achou (devido à velocidade do processamento), cria uma nova já pronta
        if (!found) {
          q.add(
            jsonEncode({
              'id_local': DateTime.now().millisecondsSinceEpoch,
              'status': 'READY_TO_SYNC_UPDATE',
              'real_id': id,
              'registroData':
                  {}, // Será preenchido pelo motor de sync se necessário
              'fotoFinalBase64': base64Encode(foto),
              'fotoFinalName': nome,
            }),
          );
        }
        await prefs.setStringList('offline_queue', q);
      }
    }
  }

  Future<Uint8List> getAnexoBytes(int id) async {
    final r = await _dio.get(
      '/registros/$id/anexo-inicio',
      options: Options(responseType: ResponseType.bytes),
    );
    return Uint8List.fromList(r.data);
  }

  Future<void> logout() async {
    await _storage.delete(key: 'jwt_token');
    await _storage.delete(key: 'user_cpf_for_reload');
  }
}
