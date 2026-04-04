// lib/models/registro_horimetro.dart

class RegistroHorimetro {
  final int id;
  final String data;
  final double valorHorimetro;
  final String? caminhoFoto;
  final String? maquinaNome; // Caso o backend retorne o nome da máquina

  RegistroHorimetro({
    required this.id,
    required this.data,
    required this.valorHorimetro,
    this.caminhoFoto,
    this.maquinaNome,
  });

  factory RegistroHorimetro.fromJson(Map<String, dynamic> json) {
    return RegistroHorimetro(
      id: json['id'] ?? 0,
      data: json['data'] ?? '',
      valorHorimetro: json['valorHorimetro'] != null
          ? (json['valorHorimetro'] as num).toDouble()
          : 0.0,
      caminhoFoto: json['caminhoFoto'],
      maquinaNome: json['maquinaNome'],
    );
  }
}
