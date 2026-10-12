# 🚗 Garagem do Tião - Servidor

Bem-vindo ao repositório do servidor da **Garagem do Tião**! O projeto foi atualizado e agora utiliza o **MySQL** como banco de dados. Siga o passo a passo abaixo para estruturar o ambiente perfeitamente.

---

## 📋 Pré-requisitos

Antes de começar, certifique-se de ter as seguintes ferramentas instaladas na sua máquina:

- **[Git](https://git-scm.com/)**
- **Java SE 22**
- **[XAMPP](https://www.apachefriends.org/pt_br/index.html)** (inclui o Apache e o MySQL)

---

## 🚀 Como Instalar e Configurar

### 1. Clonando o Repositório
Se ainda não tiver baixado o projeto, abra o seu terminal e faça o clone:

```bash
git clone [https://github.com/JoaoAltevir/GaragemDoTiaoServidor.git](https://github.com/JoaoAltevir/GaragemDoTiaoServidor.git)
```
### 2. Configurando o Banco de Dados (via XAMPP)

Todo o processo de estruturação do banco de dados será feito utilizando o painel do XAMPP e o phpMyAdmin:

1. Abra o **XAMPP Control Panel**.
2. Inicie os serviços clicando no botão **Start** ao lado de **Apache** e de **MySQL**.
3. Em seguida, clique no botão **Admin** (que fica na mesma linha do MySQL). Isso abrirá a tela do **phpMyAdmin** no seu navegador.
4. No menu lateral do phpMyAdmin, clique em "Novo" e crie um banco de dados com o exato nome: **`garagemdotiao`**.
5. Clique sobre o banco de dados recém-criado para selecioná-lo e vá até a aba **Importar** (no menu superior).
6. Clique no botão de escolher arquivo, vá até a raiz do projeto clonado e localize a pasta `database`.
7. Selecione o arquivo `.sql` que está dentro dessa pasta e confirme a importação no final da página.

Pronto! Toda a estrutura de tabelas e configurações do banco já estará rodando perfeitamente.

### 3. Configurando as Credenciais da Aplicação

*Dica rápida:* Não se esqueça de acessar o arquivo `database.properties` (ou o seu arquivo de configuração correspondente) no projeto Java e alterar a URL de conexão para o padrão do MySQL (`jdbc:mysql://localhost:3306/garagemdotiao`), atualizando também o seu usuário (geralmente `root`) e a senha (geralmente vazia no XAMPP padrão).

---

## 🎉 Pronto para testes!

Com o banco de dados estruturado e as propriedades configuradas, o servidor já pode ser executado. Inicie os testes!!
