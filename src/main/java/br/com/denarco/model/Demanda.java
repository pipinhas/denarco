package br.com.denarco.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Um problema relatado (buraco, poste apagado...), com local, situação e prioridade. */
@Entity
public class Demanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ex.: ARC-2026-000123. Gerado depois de salvar, porque usa o id.
    private String protocolo;

    @ManyToOne
    private Categoria categoria;

    @ManyToOne
    private Setor setor;

    @ManyToOne
    private Usuario autor;

    @ManyToOne
    private Usuario responsavel;

    private String descricao;

    @Enumerated(EnumType.STRING)
    private StatusDemanda status;

    @Enumerated(EnumType.STRING)
    private Urgencia urgencia;

    @Enumerated(EnumType.STRING)
    private Alcance alcance;

    private boolean riscoSeguranca;

    @Enumerated(EnumType.STRING)
    private Canal canal;

    private String solicitanteNome;
    private String solicitanteTelefone;

    private Double latitude;
    private Double longitude;
    private String endereco;
    private String bairro;
    private String pontoReferencia;

    // Nome do arquivo da foto na pasta de fotos (nunca o caminho completo).
    private String foto;

    private int pontuacao;
    private boolean pontuacaoFixada;
    private int totalApoios;
    private LocalDateTime ultimoApoioEm;

    private LocalDate prazo;
    private LocalDateTime criadaEm;
    private LocalDateTime atualizadaEm;
    private LocalDateTime concluidaEm;

    public boolean isAtrasada() {
        return prazo != null && status.isEmAberto() && LocalDate.now().isAfter(prazo);
    }

    public boolean temLocalNoMapa() {
        return latitude != null && longitude != null;
    }

    // Faixas da seção 4.3 do plano.
    public String getFaixaPrioridade() {
        if (pontuacao >= 60) return "Alta";
        if (pontuacao >= 35) return "Média";
        return "Baixa";
    }

    public String getClassePrioridade() {
        if (pontuacao >= 60) return "prioridade-alta";
        if (pontuacao >= 35) return "prioridade-media";
        return "prioridade-baixa";
    }

    public String getLocalResumido() {
        if (bairro != null && !bairro.isBlank()) return bairro;
        if (endereco != null && !endereco.isBlank()) return endereco;
        return "Local não informado";
    }

    public Long getId() {
        return id;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Setor getSetor() {
        return setor;
    }

    public void setSetor(Setor setor) {
        this.setor = setor;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Usuario responsavel) {
        this.responsavel = responsavel;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public StatusDemanda getStatus() {
        return status;
    }

    public void setStatus(StatusDemanda status) {
        this.status = status;
    }

    public Urgencia getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(Urgencia urgencia) {
        this.urgencia = urgencia;
    }

    public Alcance getAlcance() {
        return alcance;
    }

    public void setAlcance(Alcance alcance) {
        this.alcance = alcance;
    }

    public boolean isRiscoSeguranca() {
        return riscoSeguranca;
    }

    public void setRiscoSeguranca(boolean riscoSeguranca) {
        this.riscoSeguranca = riscoSeguranca;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setCanal(Canal canal) {
        this.canal = canal;
    }

    public String getSolicitanteNome() {
        return solicitanteNome;
    }

    public void setSolicitanteNome(String solicitanteNome) {
        this.solicitanteNome = solicitanteNome;
    }

    public String getSolicitanteTelefone() {
        return solicitanteTelefone;
    }

    public void setSolicitanteTelefone(String solicitanteTelefone) {
        this.solicitanteTelefone = solicitanteTelefone;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getPontoReferencia() {
        return pontoReferencia;
    }

    public void setPontoReferencia(String pontoReferencia) {
        this.pontoReferencia = pontoReferencia;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(int pontuacao) {
        this.pontuacao = pontuacao;
    }

    public boolean isPontuacaoFixada() {
        return pontuacaoFixada;
    }

    public void setPontuacaoFixada(boolean pontuacaoFixada) {
        this.pontuacaoFixada = pontuacaoFixada;
    }

    public int getTotalApoios() {
        return totalApoios;
    }

    public void setTotalApoios(int totalApoios) {
        this.totalApoios = totalApoios;
    }

    public LocalDateTime getUltimoApoioEm() {
        return ultimoApoioEm;
    }

    public void setUltimoApoioEm(LocalDateTime ultimoApoioEm) {
        this.ultimoApoioEm = ultimoApoioEm;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public void setPrazo(LocalDate prazo) {
        this.prazo = prazo;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

    public void setAtualizadaEm(LocalDateTime atualizadaEm) {
        this.atualizadaEm = atualizadaEm;
    }

    public LocalDateTime getConcluidaEm() {
        return concluidaEm;
    }

    public void setConcluidaEm(LocalDateTime concluidaEm) {
        this.concluidaEm = concluidaEm;
    }
}
