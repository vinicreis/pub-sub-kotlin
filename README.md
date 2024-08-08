# Publish/Subscribe Service

## Introdução

Este trabalho visa a implementação de um serviço de filas de mensagens utilizando o protocolo gRPC para comunicação
entre cliente e servidor. Seus elementos principais são um servidor, que gerencia as filas de mensagens e seus 
inscritos, um cliente desenvolvido em Kotlin e um cliente desenvolvido em Python.
O processo de _build_ e gerenciamento de dependências dos módulos em Kotlin são realizados utilizando o Gradle.
A publicação do servidor e _build_ dos clientes podem ser realizados utilizando o Docker e através da tarefas
declaradas no Makefile na raiz do projeto.

## Dependências

- JDK 17
- Docker e Docker Compose
- Comando `make`

## Como executar

Todos módulos neste projeto podem ser executados através do Makefile na raiz do projeto, cada um com seu target.
Confira abaixo como cada um deles pode ser executado:

- Servidor: 
  - ```shell
    make server
    ```
  
- Cliente Kotlin: 
  - ```shell
    make client
    ```
  
- Cliente Python: 
  - ```shell
    make py_client
    ```

Note que o servidor deve ser executado antes dos clientes para o correto funcionamento dos clientes. Para alterar
a porta em que o servidor será executado, altere a variável `SERVER_PORT` para o valor desejato no arquivo 
`.env-sample` na raiz do projeto.

O contâiner do servidor é compilado através da imagem gerada pelo `Dockerfile` na raiz do módulo `server`, 
que utiliza as variáveis de ambiente do arquivo `.env-sample` na raiz do projeto. Por ser um projeto acadêmico,
o arquivo de exemplo foi utilizado para facilitar a execução do projeto.

Neste arquivo `.env-sample` também é possível alterar as credenciais do banco de dados, caso desejado.

## Arquitetura do projeto

Para chegar o mais próximo de uma aplicação de produção, adotamos uma estrutura de multi-módulos para cada um dos
módulos principais. Vamos detalhar cada um dos principais e de seus submódulos.

### Servidor

O servidor conta com 2 principais submódulos, `core` e `java-app`, onde o primeiro é responsável pela 
implementação da lógica de negócio necessária para que qualquer variante da aplicação funcione. Já o módulo `java-app`
possui apenas o que é necessário para uma aplicação Java nativa possa importar esta implementação e ser executada.

Dentro do módulo `core`, o submódulo `domain` possui o modelo das entidades de domínio da aplicação. Isto é, 
modelos que os serviços e casos de uso da aplicação iram utilizar para realizar suas tarefas. O submódulo
`service` contém apenas interfaces de domínio para serviços que deveram ser implementadas por algum componente 
de infraestrutura (que será detalhado nos próximos submódulos abordados). A proposta deste tipo de prática é facilitar
a alteração entre implementações, além de simplificar a implementação de testes unitários que dependem de alguma
implementação com este contrato.

O submódulo `data` é um pouco mais complexo, mas de forma semelhante, o submódulo `repository` define as interfaces
de domínio que a aplicação pode utilizar para acessar algum modelo de dados. Enquanto o submódulo 
`database:postgres` possui a implementação desses repositórios baseado num banco de dados PostgreSQL.

Os submódulos restantes `test` e `util` são complementares e apenas definem ferramentas comuns a todos os submódulos
para testes e utilidades.

Note que o módulo `java-app` possui apenas um submódulo, `core`, pois nào é necessária nenhuma interação com o
usuário.

## Implementação

### Servidor

### Cliente 

## Cobertura de testes
