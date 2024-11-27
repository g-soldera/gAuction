# gAuction

<div style="display: flex; align-items: center;">
<img src="https://media.discordapp.net/attachments/1310952241808543815/1311409218506461204/gauction.gif?ex=6748c082&is=67476f02&hm=7633c94f1e91433e4bfb021cb686d41dd2385328440541b558ff2bf097b8e675&=" width="300" height="300" align="left" style="margin-right: 20px;">

Um sistema de leilões robusto e intuitivo para servidores Minecraft, permitindo que jogadores vendam e comprem itens através de uma interface gráfica amigável.
</div>

## 📋 Características

- **Sistema de Leilões em Tempo Real**
  - Leilões com temporizador automático e avisos
  - Sistema de lances incrementais (fixo ou porcentagem)
  - Fila de leilões organizada
  - Histórico completo de transações
  - Depósito de itens (warehouse)

- **Interface Gráfica Intuitiva**
  - Menu principal com todas as funcionalidades
  - Sistema de lances rápidos e personalizados
  - Visualização e gerenciamento da fila de leilões
  - Histórico detalhado com filtros pessoais
  - Depósito para coleta de itens

- **Recursos Administrativos**
  - Controle total sobre leilões ativos
  - Sistema de banimento de itens (item na mão ou baú)
  - Configuração flexível de durações
  - Gerenciamento da fila de leilões
  - Comandos de administração

- **Economia Integrada**
  - Suporte completo ao Vault
  - Taxa de publicação configurável
  - Sistema de reembolso automático
  - Histórico financeiro detalhado
  - Incremento de lances configurável

## 🚀 Instalação

1. Certifique-se de ter o Vault instalado
2. Baixe o arquivo `.jar` mais recente [aqui](https://github.com/g-soldera/gAuction/releases)
3. Coloque o arquivo na pasta `plugins` do seu servidor
4. Reinicie o servidor
5. Configure o plugin conforme necessário

### 📋 Dependências

- Vault
- Servidor Minecraft 1.20+
- Java 17 ou superior

## ⚙️ Configuração

### config.yml

```yaml
# Auction settings
auction:
  duration: 300 # Duração padrão dos leilões em segundos
  max_queue_size: 10 # Tamanho máximo da fila de leilões
  step:
    enabled: true # Habilita incremento mínimo
    percentage: 10.0 # Porcentagem do valor inicial
  fees:
    publication: 0.0
    bid: 0.0 # Taxa sobre o lance final
  banned_items: [] # Lista de itens banidos

# Database settings
database:
  type: SQLITE # Tipo de banco de dados (SQLITE, MYSQL)
  host: localhost # Host do banco de dados (Mysql)
  port: 3306 # Porta do banco de dados (Mysql)
  name: gauction # Nome do banco de dados (Mysql)
  user: root # Usuário do banco de dados (Mysql)
  password: "" # Senha do banco de dados (Mysql)
```

## 🎮 Comandos

### Comandos do Jogador
- `/leilao` - Abre o menu principal
- `/leilao criar [lance_min] [incremento]` - Cria um novo leilão
- `/leilao lance <valor>` - Dá um lance no leilão atual
- `/leilao info` - Mostra informações do leilão atual

### Comandos Administrativos
- `/leilaoadmin banitem` - Bane o item na mão
- `/leilaoadmin banchest` - Bane itens no baú
- `/leilaoadmin setduration <segundos>` - Define duração do leilão
- `/leilaoadmin cancelauction` - Cancela leilão atual
- `/leilaoadmin forcestart [lance min] [incremento]` - Força início do leilão
- `/leilaoadmin reload` - Recarrega configuração

## 🔒 Permissões

- `gauction.admin` - Acesso a comandos administrativos

## 📦 Funcionalidades Detalhadas

### Sistema de Leilões
- Criação via comando ou interface
- Incremento automático configurável
- Temporizador com avisos
- Sistema de fila organizado
- Cancelamento e gerenciamento

### Interface Gráfica
- Menu principal intuitivo
- Sistema de lances simplificado
- Visualização da fila de leilões
- Histórico com filtros
- Depósito de itens

### Warehouse (Depósito)
- Armazenamento automático de itens
- Coleta individual ou em massa
- Status detalhado dos itens
- Sistema de coleta seguro

## 📝 Licença

Este projeto está sob a licença MIT - veja o arquivo [LICENSE.md](LICENSE.md) para detalhes

## 👥 Autor

- **Gustavo Soldera** - *Desenvolvimento* - [gsoldera](https://github.com/g-soldera)

## 📞 Suporte

Para suporte, abra uma issue no GitHub ou entre em contato através do Discord