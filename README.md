# intellihouse

_intellihouse_ is a **smart-home**-solution based on an openHAB-server and many distributed Raspberry-Pi-nodes controlling the actual hardware.

The idea is to put one (or more) Raspberry-Pi in every room and connect the hardware to its GPIO pins. Such a node is then fully functional, even if the openHAB server is offline (e.g. for maintenance or due to a hardware crash). So, if there are local sensors inside the room (e.g. an ordinary key button or a passive infrared sensor), the room's hardware can be used without any openHAB-interaction.

Every node (both Raspberry Pis and openHAB-server) has its own **OpenPGP**-key-pair. All communication is encrypted and signed using OpenPGP!

_intellihouse_ is thus **one of the most secure** smart-home-solutions available!

Read more in the [Wiki](https://github.com/intellihouse/intellihouse/wiki).
