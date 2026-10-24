# Denarco

Sistema para registrar, encaminhar e acompanhar demandas de serviços públicos de Arcoverde.
Proposta para o desafio do Desafios PE. O plano completo está em [PLANO_DE_IMPLEMENTACAO.md](PLANO_DE_IMPLEMENTACAO.md).

## Como rodar

Só precisa do **JDK 21 ou mais novo**. O Maven vem junto (`mvnw`) e o banco de desenvolvimento (H2) é um arquivo na pasta `dados/`.

Verificar a versão do JDK instaladdo na maquina:

```CMD
javac -version
```
ou
```CMD
javac --version
```

Windows (PowerShell), ajustando o caminho do seu JDK:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"#Corrija para a versão do JDK instalada na sua máquina.
.\mvnw.cmd spring-boot:run
```

Linux ou macOS:

```bash
./mvnw spring-boot:run
```

Depois abra http://localhost:8080.

### Contas de demonstração

Todas com a senha `demo1234` (também há atalhos na tela de login):

| E-mail | Perfil |
|---|---|
| cidadao@demo.com | Cidadão |
| servidor@demo.com | Servidor (Iluminação pública) |
| gestor@demo.com | Gestor (Iluminação pública) |
| admin@demo.com | Administrador |
| pendente@demo.com | Conta corporativa esperando aprovação |
| pavimentacao@demo.com, limpeza@demo.com, drenagem@demo.com, conservacao@demo.com | Servidores dos outros setores |
| morador1@demo.com até morador12@demo.com | Moradores fictícios (autores e apoiadores das demandas de exemplo) |

As 15 demandas de exemplo são fictícias e ficam espalhadas pelo centro de Arcoverde (uma delas chegou "por telefone").
Há também um convite pendente: quem criar conta corporativa com `convidado@demo.com` entra direto como servidor da Limpeza urbana.

### Roteiro rápido para ver o ciclo completo

1. **Cidadão** registra uma demanda de Poste apagado.
2. **Gestor** (Iluminação) abre a fila, entra na demanda e define responsável e prazo.
3. **Servidor** inicia, registra uma providência com foto e conclui.
4. **Cidadão** vê a linha do tempo completa e pode reabrir em até 7 dias.
5. **Admin** aprova o pedido de acesso do Pedro em Equipe e cria um setor ou tipo de problema em Configurações.
6. **Gestor ou admin** abre o Painel (números, gráficos, bairros), pergunta "buracos abertos este mês" e vê o Mapa.

### Testar no celular (GPS precisa de HTTPS)

O navegador só libera a localização em sites HTTPS. Para testar pelo celular na mesma rede Wi-Fi:

```powershell
# Só na primeira vez: gera um certificado de demonstração (keytool vem com o JDK)
mkdir certificado
& "$env:JAVA_HOME\bin\keytool" -genkeypair -alias denarco -keyalg RSA -keysize 2048 -storetype PKCS12 `
  -keystore certificado\denarco.p12 -validity 365 -storepass denarco123 -dname "CN=Denarco"

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,https"
```

No celular, abra `https://IP-DO-COMPUTADOR:8443` e aceite o aviso de certificado (é um certificado de demonstração).

### Outros comandos

| O quê | Como |
|---|---|
| Rodar os testes | `.\mvnw.cmd test` |
| Ver o banco | http://localhost:8080/h2-console · JDBC URL `jdbc:h2:file:./dados/denarco` · usuário `sa`, sem senha |
| Começar do zero | parar o sistema e apagar a pasta `dados/` |
| "Esqueci minha senha" sem e-mail configurado | o link aparece na tela e no console (modo demonstração) |

## Produção (PostgreSQL)

Criar o banco `denarco` e rodar com o perfil `prod`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:postgresql://localhost:5432/denarco"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "..."
$env:ADMIN_EMAIL = "admin@prefeitura.exemplo"
$env:ADMIN_SENHA = "..."
.\mvnw.cmd spring-boot:run
```

E-mail real: definir `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` e `SPRING_MAIL_PASSWORD`.

## Como colaborar

1. Baixe o projeto (só na primeira vez): `git clone https://github.com/pipinhas/denarco.git`
2. Antes de começar a mexer, traga o que os colegas já enviaram: `git pull`
3. Faça a alteração e rode os testes: `.\mvnw.cmd test`
4. Envie: `git add .` → `git commit -m "o que você mudou"` → `git push`

Para mudanças grandes, prefira criar uma branch (`git switch -c minha-mudanca`) e abrir um Pull Request no GitHub,
assim outra pessoa revisa antes de entrar na `main`.
Nunca edite uma migration que já existe (`db/migration`): crie a próxima, por exemplo `V12__...sql`.

O guia completo do projeto, em linguagem simples, está em [docs/Guia_do_Projeto_Denarco.pdf](docs/Guia_do_Projeto_Denarco.pdf).

## Organização do código

```
main
 | java
 |  | config/      segurança, admin inicial e dados de demonstração
 |  | controller/  recebe a requisição e escolhe a tela
 |  | dto/         formulários
 |  | model/       entidades e enums
 |  | repository/  acesso ao banco (Spring Data JPA)
 |  | security/    login (Spring Security)
 |  | service/     regras de negócio
 | resources
 |  | db/migration/   migrations do Flyway (nunca editar uma que já rodou)
 |  | templates/      telas (Thymeleaf)
```
