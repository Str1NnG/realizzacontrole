import 'package:flutter/foundation.dart'; // Para ValueNotifier e ChangeNotifier

// Modelo simples para os dados do usuário que precisamos
class UserData {
  final int id;
  final String nome;
  final String cpf;
  final String? cargo; // Pode ser nulo
  // Adicione outros campos se precisar (email, telefone, fotoUrl, etc.)

  UserData({
    required this.id,
    required this.nome,
    required this.cpf,
    this.cargo,
  });

  // Fábrica para criar UserData a partir do Map da API
  factory UserData.fromMap(Map<String, dynamic> map) {
    return UserData(
      id: map['id'] as int,
      nome: map['nome'] as String? ?? 'Nome não encontrado', // Valor padrão
      cpf: map['cpf'] as String? ?? '---.---.---.--', // Valor padrão
      cargo: map['cargo'] as String?,
    );
  }
}

class AuthService extends ChangeNotifier {
  // Padrão Singleton
  static final AuthService _instance = AuthService._internal();
  factory AuthService() {
    return _instance;
  }
  AuthService._internal();

  // Notificador para os dados do usuário
  final ValueNotifier<UserData?> _userDataNotifier = ValueNotifier(null);

  // Getter público para acessar os dados atuais
  UserData? get currentUser => _userDataNotifier.value;

  // Getter para ouvir mudanças nos dados (para Widgets que precisam reagir)
  ValueListenable<UserData?> get userDataListenable => _userDataNotifier;

  // Método para definir o usuário após o login
  void setUser(Map<String, dynamic> userDataMap) {
    try {
      _userDataNotifier.value = UserData.fromMap(userDataMap);
      // notifyListeners(); // Desnecessário com ValueNotifier
    } catch (e) {
      print("Erro ao converter dados do usuário: $e");
      _userDataNotifier.value = null; // Garante estado nulo em caso de erro
    }
  }

  // Método para limpar o usuário no logout
  void clearUser() {
    _userDataNotifier.value = null;
    // notifyListeners(); // Desnecessário com ValueNotifier
  }
}
