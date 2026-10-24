package br.com.denarco.exception;

/**
 * Erro de regra de negócio (ex.: e-mail já cadastrado).
 * A mensagem é escrita para o usuário final, porque aparece direto na tela.
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
