// lib/models/registro_diario.dart

class RegistroDiario {
  final int id;
  final String data;
  final double horimetroFinal;
  final String? caminhoAnexo;

  RegistroDiario({
    required this.id,
    required this.data,
    required this.horimetroFinal,
    this.caminhoAnexo,
  });

  factory RegistroDiario.fromJson(Map<String, dynamic> json) {
    return RegistroDiario(
      id: json['id'] ?? 0,
      data: json['data'] ?? '',
      // Mantivemos horimetroFinal para manter o padrão antigo do seu form
      horimetroFinal: json['horimetroFinal'] != null
          ? (json['horimetroFinal'] as num).toDouble()
          : (json['valorHorimetro'] != null
                ? (json['valorHorimetro'] as num).toDouble()
                : 0.0),
      caminhoAnexo: json['caminhoAnexo'] ?? json['caminhoFoto'],
    );
  }
}
