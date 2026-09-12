# ClanPlus

Sistema de clans para Spigot/Paper, inspirado na experiência do SimpleClans e desenvolvido para a API Spigot 26.2.

## Menu principal

- `/clan` — abre o GUI principal do sistema de clans.
- `/clans` — abre o mesmo GUI principal.
- O menu mostra o perfil do jogador com clan, cargo, coins, KDR, kills e mortes.
- O menu possui atalhos para criação de clan, convites, clans do servidor, clans mais top e ranking de KDR.

## Rankings

- **Clans mais Top** — calculado pelo KDR médio dos jogadores que pertencem a cada clan.
- **Ranking de KDR** — ranking individual dos jogadores com estatísticas registradas pelo ClanPlus.
- KDR é calculado com base em kills e mortes registradas pelo plugin.

## Tags

As tags dos clans devem ter **exatamente 3 letras MAIÚSCULAS**.

Exemplos válidos: `ABC`, `DON`, `XYZ`.

Números, símbolos, espaços e letras minúsculas não são aceitos.

## Comandos

- `/clanadmin` — abre o painel administrativo.
- `/clan menu` — abre o menu da própria clan.
- `/clan tag <TAG>` — altera a tag da clan.
- `/clan config` — abre a configuração da clan.
- `/clan info [clan]` — mostra informações.
- `/clan criar <nome> <TAG>` — cria uma clan.
- `/clan excluir` — exclui a clan.
- `/clan sair` — sai da clan.
- `/clan online` — mostra membros online.
- `/clan expulsar <jogador>` — expulsa um membro.
- `/clan promover <jogador>` — promove para moderador.
- `/clan rebaixar <jogador>` — rebaixa para membro.
- `/clan transferir <jogador>` — transfere a posse.
- `/clan convidar <jogador>` — envia convite.
- `/clan convites` — abre os convites recebidos.
- `/clan aceitar <id>` — aceita um convite.
- `/clan recusar <id>` — recusa um convite.
- `/clan chat <mensagem>` — envia mensagem no chat da clan.
- `/clan bau` — abre o baú comunitário.
- `/clan sethome` — define a home.
- `/clan home` — teleporta para a home.

## Permissões

A permissão raiz é `clanplus.*`.

- `clanplus.clan` — comandos de jogador.
- `clanplus.admin` — painel administrativo.
- `clanplus.*` — acesso completo.

## Armazenamento

Os clans são persistidos em `plugins/ClanPlus/clans.yml`, incluindo membros, convites, home, configurações, conteúdo do baú comunitário e estatísticas de KDR.

## Coins

O GUI pode exibir o saldo através do PlaceholderAPI. O placeholder configurado por padrão é `%vault_eco_balance_fixed%`. Se PlaceholderAPI não estiver instalado ou o placeholder não estiver disponível, o GUI mostra `N/D`.

## Configuração

As mensagens ficam em `plugins/ClanPlus/messages.yml` e as regras principais em `config.yml`.
