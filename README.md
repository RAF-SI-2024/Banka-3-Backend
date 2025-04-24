# Banka-3: Backend

## Pravila

### Formatiranje

- Koristiti iskljucivo engleski jezik
- Koristiti camelCase

### Ostalo

- Obavezni unit testovi na svakom PR
- Obavezno koriscenje DTO za sve zahteve
- Obavezno anotiranje svih endpointa sa swagger anotacijama
- Koristiti unix timestamp

### Pre nego sto trazite PR review:

- Pokrenuti aplikaciju i uveriti se da sve radi ispravno
- Formatirati kod: npr. u InteliJ: komanda CTRL + ALT + L nad otvorenim fajlom, ili desni klik nad direktorijumom ili
  klasom i odabirom opcije Reformat Code, gde se štiklira sledeće:
    - Optimize imports
    - Rearrange entities
    - Cleanup code

### Šta je potrebno?

Trenutno zbog baze je potrebno instalirati [Docker Desktop](https://www.docker.com/products/docker-desktop/) i pokrenuti
je sa `docker compose up --build` 
