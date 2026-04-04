package com.kodarsoftwares.nivelar.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kodarsoftwares.nivelar.dto.RegistroDiarioDTO;
import com.kodarsoftwares.nivelar.model.Cliente;
import com.kodarsoftwares.nivelar.model.Operador;
import com.kodarsoftwares.nivelar.model.RegistroDiario;
import com.kodarsoftwares.nivelar.repository.ClienteRepository;
import com.kodarsoftwares.nivelar.repository.OperadorRepository;
import com.kodarsoftwares.nivelar.repository.RegistroDiarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RegistroDiarioService {

    @Autowired
    private RegistroDiarioRepository registroRepository;

    @Autowired
    private OperadorRepository operadorRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FileStorageService fileStorageService;

    private final ObjectMapper objectMapper;

    private static final Double VALOR_PADRAO_HORA = 200.0;

    public RegistroDiarioService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Transactional
    public RegistroDiarioDTO create(String registroJson, MultipartFile anexo, MultipartFile anexoInicio, MultipartFile anexoFinal) {
        RegistroDiario registro;
        try {
            JsonNode jsonNode = objectMapper.readTree(registroJson);
            registro = objectMapper.treeToValue(jsonNode, RegistroDiario.class);

            if (jsonNode.has("operadorId")) {
                Long opId = jsonNode.get("operadorId").asLong();
                Operador operador = operadorRepository.findById(opId)
                        .orElseThrow(() -> new RuntimeException("Operador não encontrado com ID: " + opId));
                registro.setOperador(operador);
            } else {
                throw new RuntimeException("operadorId é obrigatório no envio do registro.");
            }

            if (registro.getLatitude() != null && registro.getLongitude() != null) {
                List<Cliente> clientesProximos = clienteRepository.encontrarClientePorCoordenada(registro.getLatitude(), registro.getLongitude());
                if (clientesProximos != null && !clientesProximos.isEmpty()) {
                    Cliente clienteEncontrado = clientesProximos.get(0);
                    registro.setClienteEntity(clienteEncontrado);
                    registro.setCliente(clienteEncontrado.getNome());
                }
            }

            if (registro.getClienteEntity() == null && jsonNode.has("clienteId")) {
                Long clienteId = jsonNode.get("clienteId").asLong();
                Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
                registro.setClienteEntity(cliente);
                if (cliente != null) registro.setCliente(cliente.getNome());
            }

            registro.setStatusPagamento("PENDENTE");
            registro.setValorFaturado(0.0);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao desserializar dados do registro.", e);
        }

        RegistroDiario registroSalvo = registroRepository.save(registro);

        try {
            if (anexo != null && !anexo.isEmpty()) {
                registroSalvo.setCaminhoAnexo(fileStorageService.storeFile(anexo));
            }
            if (anexoInicio != null && !anexoInicio.isEmpty()) {
                registroSalvo.setCaminhoAnexoInicio(fileStorageService.storeFile(anexoInicio));
            }
            if (anexoFinal != null && !anexoFinal.isEmpty()) {
                registroSalvo.setCaminhoAnexoFinal(fileStorageService.storeFile(anexoFinal));
            }
            registroSalvo = registroRepository.save(registroSalvo);
        } catch (Exception e) {
            registroRepository.delete(registroSalvo);
            throw new RuntimeException("Falha ao armazenar o arquivo. Registro foi revertido.", e);
        }

        return convertToDto(registroSalvo);
    }

    @Transactional(readOnly = true)
    public List<RegistroDiarioDTO> findAll() {
        // Ordena primeiro pela Data (mais recente) e depois pelo ID (último a ser recebido)
        return registroRepository.findAll(Sort.by(Sort.Direction.DESC, "data", "id")).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<RegistroDiarioDTO> findById(Long id) {
        return registroRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public RegistroDiario update(Long id, RegistroDiario registroDetails) {
        RegistroDiario registro = registroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado com id: " + id));

        if (registroDetails.getClienteEntity() != null) {
            registro.setClienteEntity(registroDetails.getClienteEntity());
        }

        // =================================================================
        // A SOLUÇÃO DEFINITIVA (BLINDAGEM TOTAL DO FECHAMENTO)
        // =================================================================
        if (registroDetails.getHorimetroFinal() != null) {
            registro.setHorimetroFinal(registroDetails.getHorimetroFinal());
        } else {
            // Se o app mandou NULO, nós vamos ignorar o banco de dados e forçar 
            // a injeção de um valor para que o celular entenda que encerrou!
            Double valorInicial = registro.getHorimetroInicial() != null ? registro.getHorimetroInicial() : 0.0;
            registro.setHorimetroFinal(valorInicial + 0.1); 
        }

        if (registro.getHorimetroInicial() == null) {
            registro.setHorimetroInicial(0.0);
        }
        // =================================================================

        // FATURAMENTO
        if (registro.getHorimetroFinal() >= registro.getHorimetroInicial()) {
            double horasTrabalhadas = registro.getHorimetroFinal() - registro.getHorimetroInicial();
            double valorAplicado = VALOR_PADRAO_HORA;
            if (registro.getClienteEntity() != null && registro.getClienteEntity().getValorHora() != null) {
                valorAplicado = registro.getClienteEntity().getValorHora();
            }
            registro.setValorFaturado(horasTrabalhadas * valorAplicado);
        }

        if (registroDetails.getAbastecimento() != null) {
            registro.setAbastecimento(registroDetails.getAbastecimento());
        }
        if (registroDetails.getDescricao() != null) {
            registro.setDescricao(registroDetails.getDescricao());
        }
        if (registroDetails.getServico() != null) {
            registro.setServico(registroDetails.getServico());
        }
        if (registroDetails.getCliente() != null) {
            // Só aceita o texto do celular se NÃO tivermos achado o cliente via GPS
            if (registro.getClienteEntity() == null) {
                registro.setCliente(registroDetails.getCliente());
            }
        }
        if (registroDetails.getData() != null) {
            registro.setData(registroDetails.getData());
        }
        if (registroDetails.getLatitude() != null) {
            registro.setLatitude(registroDetails.getLatitude());
        }
        if (registroDetails.getLongitude() != null) {
            registro.setLongitude(registroDetails.getLongitude());
        }

        if (registro.getClienteEntity() == null && registro.getLatitude() != null && registro.getLongitude() != null) {
            List<Cliente> clientesProximos = clienteRepository.encontrarClientePorCoordenada(registro.getLatitude(), registro.getLongitude());
            if (clientesProximos != null && !clientesProximos.isEmpty()) {
                Cliente clienteEncontrado = clientesProximos.get(0);
                registro.setClienteEntity(clienteEncontrado);
                registro.setCliente(clienteEncontrado.getNome());

                if (registro.getHorimetroFinal() >= registro.getHorimetroInicial()) {
                    double horasTrabalhadas = registro.getHorimetroFinal() - registro.getHorimetroInicial();
                    double valorAplicado = clienteEncontrado.getValorHora() != null ? clienteEncontrado.getValorHora() : VALOR_PADRAO_HORA;
                    registro.setValorFaturado(horasTrabalhadas * valorAplicado);
                }
            }
        }

        if (registroDetails.getHorimetroInicial() != null) {
            registro.setHorimetroInicial(registroDetails.getHorimetroInicial());
        }
        if (registroDetails.getStatusPagamento() != null) {
            registro.setStatusPagamento(registroDetails.getStatusPagamento());
        }
        if (registroDetails.getValorFaturado() != null && registroDetails.getValorFaturado() > 0) {
             registro.setValorFaturado(registroDetails.getValorFaturado());
        }

        return registroRepository.save(registro);
    }

    @Transactional
    public void marcarComoPago(List<Long> ids) {
        List<RegistroDiario> registros = registroRepository.findAllById(ids);
        for (RegistroDiario r : registros) {
            r.setStatusPagamento("PAGO");
        }
        registroRepository.saveAll(registros);
    }

    @Transactional
    public void deleteById(Long id) {
        RegistroDiario registro = registroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado com id: " + id));

        fileStorageService.deleteFile(registro.getCaminhoAnexo());
        fileStorageService.deleteFile(registro.getCaminhoAnexoInicio());
        fileStorageService.deleteFile(registro.getCaminhoAnexoFinal());
        registroRepository.delete(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroDiarioDTO> findByOperadorId(Long operadorId) {
        return registroRepository.findByOperadorIdOrderByDataDesc(operadorId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveAnexo(Long registroId, MultipartFile file) {
        RegistroDiario registro = registroRepository.findById(registroId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado com id: " + registroId));
        fileStorageService.deleteFile(registro.getCaminhoAnexo());
        try {
            String filename = fileStorageService.storeFile(file);
            registro.setCaminhoAnexo(filename);
            registroRepository.save(registro);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao armazenar o arquivo.", e);
        }
    }

    @Transactional
    public void deleteAnexo(Long registroId) {
        RegistroDiario registro = registroRepository.findById(registroId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado com id: " + registroId));
        boolean deleted = fileStorageService.deleteFile(registro.getCaminhoAnexo());
        if (deleted) {
            registro.setCaminhoAnexo(null);
            registroRepository.save(registro);
        } else {
            throw new RuntimeException("Falha ao deletar o arquivo físico.");
        }
    }

    @Transactional
    public void saveAnexoInicio(Long registroId, MultipartFile file) {
        RegistroDiario registro = registroRepository.findById(registroId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado com id: " + registroId));
        fileStorageService.deleteFile(registro.getCaminhoAnexoInicio());
        try {
            String filename = fileStorageService.storeFile(file);
            registro.setCaminhoAnexoInicio(filename);
            registroRepository.save(registro);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao armazenar o anexo de início.", e);
        }
    }

    @Transactional
    public void saveAnexoFinal(Long registroId, MultipartFile file) {
        RegistroDiario registro = registroRepository.findById(registroId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado com id: " + registroId));
        fileStorageService.deleteFile(registro.getCaminhoAnexoFinal());
        try {
            String filename = fileStorageService.storeFile(file);
            registro.setCaminhoAnexoFinal(filename);
            registroRepository.save(registro);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao armazenar o anexo final.", e);
        }
    }

    @Transactional
    public void deleteAnexoInicio(Long registroId) {
        RegistroDiario registro = registroRepository.findById(registroId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado com id: " + registroId));
        boolean deleted = fileStorageService.deleteFile(registro.getCaminhoAnexoInicio());
        if (deleted) {
            registro.setCaminhoAnexoInicio(null);
            registroRepository.save(registro);
        } else {
            throw new RuntimeException("Falha ao deletar o anexo de início.");
        }
    }

    @Transactional
    public void deleteAnexoFinal(Long registroId) {
        RegistroDiario registro = registroRepository.findById(registroId)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado com id: " + registroId));
        boolean deleted = fileStorageService.deleteFile(registro.getCaminhoAnexoFinal());
        if (deleted) {
            registro.setCaminhoAnexoFinal(null);
            registroRepository.save(registro);
        } else {
            throw new RuntimeException("Falha ao deletar o anexo final.");
        }
    }

    @Transactional(readOnly = true)
    public Resource loadFileAsResource(Long registroId, String tipoAnexo) {
        try {
            RegistroDiario registro = registroRepository.findById(registroId)
                    .orElseThrow(() -> new RuntimeException("Registro não encontrado"));

            String caminhoAnexo;
            switch (tipoAnexo.toLowerCase()) {
                case "principal":
                    caminhoAnexo = registro.getCaminhoAnexo();
                    break;
                case "inicio":
                    caminhoAnexo = registro.getCaminhoAnexoInicio();
                    break;
                case "final":
                    caminhoAnexo = registro.getCaminhoAnexoFinal();
                    break;
                default:
                    throw new RuntimeException("Tipo de anexo inválido: " + tipoAnexo);
            }

            if (caminhoAnexo == null || caminhoAnexo.isEmpty()) {
                throw new RuntimeException("Registro não possui este anexo.");
            }

            return fileStorageService.loadFileAsResource(caminhoAnexo);

        } catch (Exception ex) {
            throw new RuntimeException("Erro ao ler o arquivo: " + ex.getMessage());
        }
    }

    // =================================================================
    // MÁGICA RETROATIVA: VINCULA REGISTROS ANTIGOS AO NOVO CLIENTE
    // =================================================================
    @Transactional
    public void vincularRegistrosAntigosAoCliente(Cliente cliente) {
        System.out.println("\n=== [DEBUG] INICIANDO CAÇADA RETROATIVA ===");
        System.out.println("1. Cliente alvo: " + cliente.getNome());
        System.out.println("2. Lat recebida: " + cliente.getLatitude() + " | Lon recebida: " + cliente.getLongitude());

        if (cliente.getLatitude() == null || cliente.getLongitude() == null) {
            System.out.println("❌ ABORTANDO: O Angular não enviou o GPS deste cliente!");
            System.out.println("===========================================\n");
            return; // Se a obra não tem GPS, não faz nada
        }

        Double raio = cliente.getRaioTolerancia() != null ? cliente.getRaioTolerancia() : 500.0;
        System.out.println("3. Raio de busca configurado: " + raio + " metros");

        // Busca no passado todos os registros "CLIENTE GERAL" que foram feitos neste raio
        List<RegistroDiario> registrosProximos = registroRepository.encontrarRegistrosSemClienteProximos(
                cliente.getLatitude(), cliente.getLongitude(), raio
        );

        System.out.println("4. Registros órfãos encontrados no raio: " + registrosProximos.size());

        if (registrosProximos.isEmpty()) {
            System.out.println("❌ ABORTANDO: Nenhum registro antigo no raio de " + raio + "m precisou ser atualizado.");
            System.out.println("===========================================\n");
            return;
        }

        System.out.println("🚜 MÁGICA ATIVADA: Atualizando e recalculando faturamento de " + registrosProximos.size() + " registros antigos!");

        for (RegistroDiario registro : registrosProximos) {
            registro.setClienteEntity(cliente);
            registro.setCliente(cliente.getNome());

            // Recalcula o dinheiro cobrado dos serviços antigos também!
            if (registro.getHorimetroInicial() != null && registro.getHorimetroFinal() != null && registro.getHorimetroFinal() >= registro.getHorimetroInicial()) {
                double horasTrabalhadas = registro.getHorimetroFinal() - registro.getHorimetroInicial();
                double valorAplicado = cliente.getValorHora() != null ? cliente.getValorHora() : VALOR_PADRAO_HORA;
                registro.setValorFaturado(horasTrabalhadas * valorAplicado);
            }
        }

        registroRepository.saveAll(registrosProximos); // Salva todos no banco de uma vez!
        System.out.println("✅ Todos os registros antigos foram vinculados à obra: " + cliente.getNome());
        System.out.println("===========================================\n");
    }

    // =================================================================
    // LANÇAMENTO MANUAL (MODO ADMINISTRADOR)
    // =================================================================
    @Transactional
    public RegistroDiarioDTO createManual(RegistroDiario registro) {
        if (registro.getOperador() != null && registro.getOperador().getId() != null) {
            Operador operador = operadorRepository.findById(registro.getOperador().getId())
                    .orElseThrow(() -> new RuntimeException("Operador não encontrado"));
            registro.setOperador(operador);
        } else {
            throw new RuntimeException("Obrigatório selecionar um operador.");
        }

        if (registro.getClienteEntity() != null && registro.getClienteEntity().getId() != null) {
            Cliente cliente = clienteRepository.findById(registro.getClienteEntity().getId()).orElse(null);
            registro.setClienteEntity(cliente);
            if (cliente != null) registro.setCliente(cliente.getNome());
        }

        if (registro.getHorimetroInicial() == null) registro.setHorimetroInicial(0.0);
        if (registro.getHorimetroFinal() == null) registro.setHorimetroFinal(0.0);
        
        // Se deixares o valor em branco no painel, o Java calcula a matemática sozinho
        if (registro.getValorFaturado() == null || registro.getValorFaturado() <= 0) {
            if (registro.getHorimetroFinal() >= registro.getHorimetroInicial()) {
                double horas = registro.getHorimetroFinal() - registro.getHorimetroInicial();
                double valorAplicado = (registro.getClienteEntity() != null && registro.getClienteEntity().getValorHora() != null) 
                        ? registro.getClienteEntity().getValorHora() : VALOR_PADRAO_HORA;
                registro.setValorFaturado(horas * valorAplicado);
            }
        }

        if (registro.getStatusPagamento() == null) {
            registro.setStatusPagamento("PENDENTE");
        }

        RegistroDiario salvo = registroRepository.save(registro);
        return convertToDto(salvo);
    }

    private RegistroDiarioDTO convertToDto(RegistroDiario registro) {
        RegistroDiarioDTO dto = new RegistroDiarioDTO();
        dto.setId(registro.getId());
        dto.setData(registro.getData());
        dto.setHorimetroInicial(registro.getHorimetroInicial());
        dto.setHorimetroFinal(registro.getHorimetroFinal());
        dto.setAbastecimento(registro.getAbastecimento());
        dto.setDescricao(registro.getDescricao());
        dto.setCaminhoAnexo(registro.getCaminhoAnexo());

        dto.setServico(registro.getServico());
        dto.setCliente(registro.getCliente());
        dto.setLatitude(registro.getLatitude());
        dto.setLongitude(registro.getLongitude());
        dto.setCaminhoAnexoInicio(registro.getCaminhoAnexoInicio());
        dto.setCaminhoAnexoFinal(registro.getCaminhoAnexoFinal());

        dto.setStatusPagamento(registro.getStatusPagamento());
        dto.setValorFaturado(registro.getValorFaturado());

        dto.setDataCriacao(registro.getDataCriacao());
        dto.setDataAtualizacao(registro.getDataAtualizacao());

        if (registro.getOperador() != null) {
            RegistroDiarioDTO.OperadorInfo oInfo = new RegistroDiarioDTO.OperadorInfo();
            oInfo.setId(registro.getOperador().getId());
            oInfo.setNome(registro.getOperador().getNome());

            if (registro.getOperador().getMaquina() != null) {
                RegistroDiarioDTO.MaquinaInfo mInfo = new RegistroDiarioDTO.MaquinaInfo();
                mInfo.setNome(registro.getOperador().getMaquina().getNome());
                oInfo.setMaquina(mInfo);
            }
            dto.setOperador(oInfo);
        }

        if (registro.getClienteEntity() != null) {
            RegistroDiarioDTO.ClienteInfo cInfo = new RegistroDiarioDTO.ClienteInfo();
            cInfo.setId(registro.getClienteEntity().getId());
            cInfo.setNome(registro.getClienteEntity().getNome());
            cInfo.setLocalServico(registro.getClienteEntity().getLocalServico());
            cInfo.setValorHora(registro.getClienteEntity().getValorHora());
            dto.setClienteDetalhe(cInfo);
        }

        return dto;
    }
}