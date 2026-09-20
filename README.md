# ClanPlus

O ClanPlus é o sistema de clans do meu servidor.

A ideia é ter tudo pelo próprio menu, sem precisar ficar decorando um monte de comando. O jogador consegue criar a clan, administrar os membros, ver rankings, usar o baú e configurar a home pelo GUI.

## Menu principal

- `/clan` abre o menu principal.
- `/clans` também abre o menu principal.
- O menu mostra as informações do jogador.
- Mostra clan, cargo, coins, KDR, kills e mortes.
- Atalhos para criar clan, ver convites, clans do servidor, clans em destaque e ranking de KDR.

## Rankings

### Clans mais Top

O ranking das clans é calculado usando o KDR médio dos jogadores que fazem parte de cada clan.

### Ranking de KDR

Mostra os jogadores com as estatísticas registradas pelo ClanPlus.

O KDR é calculado usando kills e mortes registradas pelo próprio plugin.

## Tags das clans

As tags precisam ter **3 letras maiúsculas** para jogadores comuns. O cargo **DEV** pode criar e alterar tags com até **5 letras maiúsculas**.

Exemplos:

- `ABC`
- `DON`
- `XYZ`

Para DEV, também são aceitas tags como `DONNY` ou `ADMIN`.

Não são aceitos números, símbolos, espaços ou letras minúsculas na tag.

## Comandos

| Comando | O que faz |
|---|---|
| `/clan` | Abre o menu principal. |
| `/clans` | Abre o menu principal. |
| `/clanadmin` | Abre o painel administrativo. |
| `/clan menu` | Abre o menu da própria clan. |
| `/clan tag <TAG>` | Altera a tag da clan. |
| `/clan config` | Abre as configurações da clan. |
| `/clan info [clan]` | Mostra informações da clan. |
| `/clan criar <nome> <TAG>` | Cria uma clan. |
| `/clan excluir` | Exclui a clan. |
| `/clan sair` | Sai da clan. |
| `/clan online` | Mostra os membros online. |
| `/clan expulsar <jogador>` | Expulsa um membro. |
| `/clan promover <jogador>` | Promove um membro para moderador. |
| `/clan rebaixar <jogador>` | Rebaixa um moderador para membro. |
| `/clan transferir <jogador>` | Transfere a posse da clan. |
| `/clan convidar <jogador>` | Envia convite para um jogador. |
| `/clan convites` | Abre os convites recebidos. |
| `/clan aceitar <id>` | Aceita um convite. |
| `/clan recusar <id>` | Recusa um convite. |
| `/clan chat <mensagem>` | Envia mensagem no chat da clan. |
| `/clan bau` | Abre o baú comunitário. |
| `/clan sethome` | Define a home da clan. |
| `/clan home` | Teleporta para a home da clan. |

## Permissões

| Permissão | O que faz |
|---|---|
| `clanplus.clan` | Permite usar os comandos de jogador. |
| `clanplus.admin` | Permite acessar o painel administrativo. |
| `clanplus.*` | Dá acesso completo ao plugin. |

A permissão `clanplus.admin` é destinada ao cargo **DEV**. A permissão `clanplus.clan` fica para os demais jogadores que tiverem acesso ao sistema de clans.

## Armazenamento

Os dados das clans ficam em:

`plugins/ClanPlus/clans.yml`

Ali ficam membros, convites, home, configurações, baú comunitário e estatísticas de KDR.

## Coins

O menu pode mostrar o saldo do jogador usando PlaceholderAPI.

Por padrão é usado:

`%vault_eco_balance_fixed%`

Se o PlaceholderAPI ou o placeholder não estiver disponível, o menu mostra `N/D`.

## Configuração

As mensagens ficam em:

`plugins/ClanPlus/messages.yml`

As configurações principais ficam em:

`plugins/ClanPlus/config.yml`

## Plataforma

- Java 26
- Spigot API 26.2
- Maven

## Status

O ClanPlus está em desenvolvimento. A ideia é deixar o sistema de clans completo pelo GUI e ir adicionando os recursos que fizerem sentido para o servidor sem deixar o jogador preso em comandos complicados.
