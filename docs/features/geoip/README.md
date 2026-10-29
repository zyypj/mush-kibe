**English** | [Português](README.pt-BR.md)

# Account location (GeoIP)

After each login, the proxy finds the approximate country, region, city and network of the player's address in the free MaxMind GeoLite2 databases and saves them on the account, where `/account` shows them. It lives in the `core:proxy` package `br.com.muxe.core.proxy.geoip`.

## User stories

- As a staff member, I want to see where an account usually connects from, so that I can spot shared or stolen accounts.
- As an operator, I want the databases to download and update themselves, so that I only paste the MaxMind keys once.
- As a player, I want my address itself never to be stored.

## How it works

- After a successful login (`PostLoginEvent`), the proxy takes the connection address and, on the I/O executor, never on the login path:
  - ignores private, loopback, link-local, carrier-grade NAT, multicast and reserved addresses;
  - looks it up in GeoLite2-City and GeoLite2-ASN;
  - dispatches `SaveAccountLocationCommand` with the country, the region (first subdivision) and the city in Portuguese (`pt-BR`, falling back to English), and the network as `AS<number> <organization>` (for example `AS28573 Claro NXT Telecomunicacoes Ltda`).
- Nothing is saved when the address is not found. A failed lookup logs one warning with the player id and the error type, never the address.
- The address itself is never stored or logged. Only the names above are kept, replacing the previous ones.

### Databases

- The proxy downloads `GeoLite2-City` and `GeoLite2-ASN` from MaxMind's official endpoint (`https://download.maxmind.com/geoip/databases/<edition>/download?suffix=tar.gz`, HTTP basic authentication with the account id and license key, then the redirect it answers without the key), extracts the `.mmdb` from the `tar.gz` and keeps it in `plugins/muxe-proxy/geoip/`.
- At start-up and every 6 hours the proxy checks the files; a file older than `geoip.refresh-after` (7 days) is downloaded again. The new file replaces the old one only after a complete download, so a failed update keeps the previous database.
- If the downloads fail, the proxy keeps running and locations stay as they are.

### Setting the MaxMind keys

1. Create a free account at maxmind.com, open **Manage License Keys** and generate a license key. Note the **account id** shown on the same page.
2. **In the config file (main way for the Pterodactyl panel):** open `plugins/muxe-proxy/config.yml` in the panel's file manager and write the values:

   ```yaml
   geoip:
     enabled: true
     account-id: "123456"
     license-key: "your-license-key"
     refresh-after: 7d
   ```

   Restart the proxy. The file holds a secret: do not share it, do not commit it and do not paste it in tickets.
3. **Alternative, environment variables:** where the host lets you add variables, write `account-id: env:MUXE_MAXMIND_ACCOUNT_ID` and `license-key: env:MUXE_MAXMIND_LICENSE_KEY` and set those variables for the proxy process. `file:/path` also works, as for every secret.

While both values are empty (the shipped default), the proxy logs once `GeoIP is off: ...` and keeps working. The license key is never logged: the credentials hide it in `toString`, and download errors only name the config keys to check.

## Commands

None. `/account` on the game servers shows the saved location.

## Permissions

None.

## Configuration

Proxy `plugins/muxe-proxy/config.yml`:

| Key | Default | Description |
|---|---|---|
| `geoip.enabled` | `true` | Turns the lookup on when the keys are set. |
| `geoip.account-id` | `""` | MaxMind account id, written directly or as `env:NAME` / `file:/path`. |
| `geoip.license-key` | `""` | MaxMind license key, written directly or as `env:NAME` / `file:/path`. Secret. |
| `geoip.refresh-after` | `7d` | Age after which a database is downloaded again. |

## Packets

None.

## Data

- The location columns of the account (country, region, city, network), written by `SaveAccountLocationCommand`.
- `plugins/muxe-proxy/geoip/GeoLite2-City.mmdb` and `GeoLite2-ASN.mmdb`.

## Metrics

None.

## Integrations

- MaxMind GeoLite2 through the GeoIP2 Java library (`com.maxmind.geoip2:geoip2`), shaded and relocated to `br.com.muxe.libs`.
- [Accounts](../accounts/README.md): `SaveAccountLocationCommand` and `FindAccountDetailsQuery`.

## Acceptance criteria

- [ ] With valid keys, the proxy downloads both databases on the first start and a player from a public address gets a location in `/account`.
- [ ] A player from `127.0.0.1` or a private network gets no lookup.
- [ ] With empty keys, the proxy logs `GeoIP is off` once and players log in normally.
- [ ] A database 7 days old is downloaded again; a broken download keeps the old file.
- [ ] Neither the address nor the license key appears in the logs.

## Failures and recovery

- **MaxMind unreachable or wrong keys:** a warning per edition (`401`/`403` say which keys to check); existing files keep being used; the next check runs 6 hours later.
- **Database down when saving:** the command fails, a warning is logged, and the next login tries again.

## Known limitations

- GeoLite2 is approximate: cities can be wrong and mobile or VPN networks often show the provider's location.
- Players behind a proxy or VPN show the location of that service.
- MaxMind's license requires keeping the databases up to date; the weekly refresh does that while the keys are valid.
