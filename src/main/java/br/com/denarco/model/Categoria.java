package br.com.denarco.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Tipo de problema (ex.: Poste apagado). Define para qual setor a demanda vai e o prazo sugerido. */
@Entity
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    // Nome do ícone do Bootstrap Icons (ex.: bi-lightbulb-off).
    private String icone;

    @ManyToOne
    private Setor setorPadrao;

    private int prazoPadraoDias;
    private boolean ativa = true;

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public Setor getSetorPadrao() {
        return setorPadrao;
    }

    public void setSetorPadrao(Setor setorPadrao) {
        this.setorPadrao = setorPadrao;
    }

    public int getPrazoPadraoDias() {
        return prazoPadraoDias;
    }

    public void setPrazoPadraoDias(int prazoPadraoDias) {
        this.prazoPadraoDias = prazoPadraoDias;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
}
