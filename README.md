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
  - Download do pacote
    - [OpenJDK 17.0.12](https://builds.openlogic.com/downloadJDK/openlogic-openjdk/17.0.12+7/openlogic-openjdk-17.0.12+7-linux-x64.tar.gz)
  - Intalação via `apt` (Ubuntu)
    - ```shell
      sudo apt install openjdk-17-jdk
      ```
  - Instalação via Pacman (ArchLinux)
    - ```shell
      pacman -S openjdk-17-jdk
      ```
- Docker e Docker Compose - [Get Docker](https://docs.docker.com/get-docker/)
- Comando `make`

## Compilação

Todos módulos neste projeto podem ser compilados através do Makefile na raiz do projeto. Confira as instruções de 
cada um deles:

### Servidor

O comando abaixo executa através do Gradle Wrapper o comando de _build_ dos pacotes executáveis 
de distribuição do servidor:

```shell
make server_build
```

Feito isso, através do comando a seguir, a imagem de um container contendo estes executáveis é criada e executada em
modo _detached_ através do Docker compose:

```shell
make server_deploy
```

Após a execução deste comando, o servidor estará disponível na porta `10090` por padrão.

### Cliente Kotlin
