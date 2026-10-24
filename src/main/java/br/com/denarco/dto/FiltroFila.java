package br.com.denarco.dto;

/** Filtros da fila, lidos da URL (ex.: /atendimento?aba=ATRASADAS&categoriaId=3). */
public class FiltroFila {

    private AbaFila aba = AbaFila.EM_ABERTO;
    private Long categoriaId;
    private String bairro;

    // Só vale para quem enxerga todos os setores (admin e planejamento).
    private Long setorId;

    private int pagina;

    public AbaFila getAba() {
        return aba;
    }

    public void setAba(AbaFila aba) {
        this.aba = aba == null ? AbaFila.EM_ABERTO : aba;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public Long getSetorId() {
        return setorId;
    }

    public void setSetorId(Long setorId) {
        this.setorId = setorId;
    }

    public int getPagina() {
        return pagina;
    }

    public void setPagina(int pagina) {
        this.pagina = Math.max(0, pagina);
    }
}
