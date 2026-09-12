# ClanPlus

Sistema de clans para Spigot/Paper, inspirado na experiência do SimpleClans e desenvolvido para a API Spigot 26.2.

## Comandos

- `/clans` — abre o localizador/menu de clans.
- `/clanadmin` — abre o painel administrativo no chat.
- `/clan` — abre o painel da clan no chat.
- `/clan menu` — abre o menu da clan.
- `/clan tag <tag>` — personaliza a tag.
- `/clan config` — abre a configuração da clan.
- `/clan info [clan]` — mostra informações.
- `/clan criar <nome> [tag]` — cria uma clan.
- `/clan excluir` — exclui a clan.
- `/clan sair` — sai da clan.
- `/clan online` — mostra membros online.
- `/clan expulsar <jogador>` — expulsa um membro.
- `/clan promover <jogador>` — promove para moderador.
- `/clan rebaixar <jogador>` — rebaixa para membro.
- `/clan transferir <jogador>` — transfere a posse.
- `/clan convidar <jogador>` — envia convite.
- `/clan convites` — consulta convites recebidos.
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

Os clans são persistidos em `plugins/ClanPlus/clans.yml`, incluindo membros, convites, home, configurações e conteúdo do baú comunitário.

## Configuração

As mensagens ficam em `plugins/ClanPlus/messages.yml` e as regras principais em `config.yml`.
