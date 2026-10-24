package br.com.denarco.service;

import br.com.denarco.exception.NegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda as fotos das demandas numa pasta fora do site público.
 * Toda foto é redesenhada como JPG de no máximo 1600px: isso reduz o tamanho
 * e descarta os metadados (localização GPS e modelo do celular de quem tirou).
 */
@Service
public class FotoService {

    private static final int LADO_MAXIMO = 1600;

    private final Path pasta;

    public FotoService(@Value("${app.pasta-fotos}") String pasta) {
        this.pasta = Path.of(pasta);
        try {
            Files.createDirectories(this.pasta);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de fotos: " + pasta, e);
        }
    }

    /** Salva a foto e devolve o nome do arquivo, ou null se nenhuma foto foi enviada. */
    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }

        BufferedImage original;
        try (InputStream entrada = arquivo.getInputStream()) {
            original = ImageIO.read(entrada);
        } catch (IOException e) {
            throw new NegocioException("Não conseguimos ler a foto. Tente outra imagem.");
        }
        if (original == null) {
            throw new NegocioException("Envie a foto em JPG ou PNG.");
        }

        double escala = Math.min(1.0, (double) LADO_MAXIMO / Math.max(original.getWidth(), original.getHeight()));
        int largura = (int) Math.round(original.getWidth() * escala);
        int altura = (int) Math.round(original.getHeight() * escala);

        BufferedImage nova = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D desenho = nova.createGraphics();
        desenho.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // Fundo branco para PNG com transparência (JPG não tem transparência).
        desenho.drawImage(original, 0, 0, largura, altura, Color.WHITE, null);
        desenho.dispose();

        String nome = UUID.randomUUID() + ".jpg";
        try {
            ImageIO.write(nova, "jpg", pasta.resolve(nome).toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar a foto", e);
        }
        return nome;
    }

    public Path caminho(String nome) {
        return pasta.resolve(nome);
    }
}
