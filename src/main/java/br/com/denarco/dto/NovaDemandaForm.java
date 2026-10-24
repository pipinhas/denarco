package br.com.denarco.dto;

import br.com.denarco.model.Alcance;
import br.com.denarco.model.Canal;
import br.com.denarco.model.Urgencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/** Dados da tela "Nova demanda". Urgência e alcance já vêm com valor padrão (não contam como clique). */
public class NovaDemandaForm {

    @NotNull(message = "Escolha o tipo de problema.")
    private Long categoriaId;

    @NotBlank(message = "Conte em poucas palavras o que está acontecendo.")
    @Size(max = 1000, message = "Use no máximo 1000 caracteres.")
    private String descricao;

    private Double latitude;
    private Double longitude;

    @Size(max = 200, message = "Use no máximo 200 caracteres.")
    private String endereco;

    @Size(max = 100, message = "Use no máximo 100 caracteres.")
    private String bairro;

    @Size(max = 200, message = "Use no máximo 200 caracteres.")
    private String pontoReferencia;

    @NotNull
    private Urgencia urgencia = Urgencia.MEDIA;

    @NotNull
    private Alcance alcance = Alcance.RUA;

    private boolean riscoSeguranca;

    private MultipartFile foto;

    // Só no registro feito por atendente (demanda recebida por telefone, presencial ou WhatsApp).
    private Canal canal;

    @Size(max = 120, message = "Use no máximo 120 caracteres.")
    private String solicitanteNome;

    @Size(max = 20, message = "Use no máximo 20 caracteres.")
    private String solicitanteTelefone;

    // Precisa de um ponto no mapa ou, se o mapa/GPS falhar, de um endereço ou referência escrita.
    public boolean temLocal() {
        return (latitude != null && longitude != null)
                || (endereco != null && !endereco.isBlank())
                || (pontoReferencia != null && !pontoReferencia.isBlank());
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
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

    public MultipartFile getFoto() {
        return foto;
    }

    public void setFoto(MultipartFile foto) {
        this.foto = foto;
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
}
