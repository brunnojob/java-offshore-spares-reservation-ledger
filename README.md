# java-offshore-spares-reservation-ledger

Replay idempotent stock reservations, releases and issues while preventing negative spare inventory.

```sh
javac Main.java
java Main --self-test
java Main < examples/input.txt
```

Offline analysis of supplied records. Results depend on the installation data and configured engineering assumptions.

MIT. Built by [brunnodev](https://brunnodev.store).
