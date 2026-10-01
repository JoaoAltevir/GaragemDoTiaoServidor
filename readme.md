
# 🚗 Garagem do Tião - Servidor

Bem-vindo ao repositório do servidor da **Garagem do Tião**! Para garantir o bom funcionamento da aplicação, siga o passo a passo abaixo para estruturar o ambiente e o banco de dados.

---

## 📋 Pré-requisitos

Antes de começar, certifique-se de ter as seguintes ferramentas instaladas na sua máquina:

- **[Git](https://git-scm.com/)**
- **Java SE 22**
- **[PostgreSQL e PgAdmin4](https://www.postgresql.org/download/)**

---

## 🚀 Configurações Iniciais

### 1. Clonando o Repositório
Abra o seu terminal e faça o clone do projeto:

```bash
git clone [https://github.com/JoaoAltevir/GaragemDoTiaoServidor.git](https://github.com/JoaoAltevir/GaragemDoTiaoServidor.git)
```

### 2. Configurando o Banco de Dados (via PgAdmin4)

Todo o processo de estruturação do banco será feito pelo PgAdmin4:

1. Abra o **PgAdmin4**, conecte-se ao seu servidor local e **crie a base de dados** que será utilizada pela aplicação.
2. Clique com o botão direito sobre a base de dados recém-criada e abra a **Query Tool**.
3. Pressione `Ctrl + O` (ou clique no ícone de abrir arquivo).
4. Navegue até a pasta do projeto clonado, acesse a pasta `database` e selecione o arquivo `GaragemDoTiao.sql`.
5. Pressione `Alt + F5` (ou o botão de "Execute/Play") para rodar a query e criar toda a estrutura de tabelas.

### 3. Configurando as Credenciais

Para que a aplicação Java consiga se comunicar com o banco de dados, você precisa informar as suas credenciais locais:

- Localize o arquivo `database.properties` na estrutura do projeto.
- Altere os dados de conexão (como `user`, `password` e a URL do banco) conforme as configurações do PostgreSQL da sua máquina.

---

## 🎉 Pronto para testes!

Com o banco de dados estruturado e as propriedades configuradas, o servidor já está pronto para ser executado e testado.


