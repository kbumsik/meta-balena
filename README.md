# Resin.io layers for Yocto

## Description
This repository enables building resin.io for various devices.

## Layers Structure
* meta-resin-common : layer which contains common recipes for all our supported platforms.
* meta-resin-* : layers which contain recipes specific to yocto versions.
* other files : README, COPYING, etc.

## Dependencies

* http://www.yoctoproject.org/docs/latest/yocto-project-qs/yocto-project-qs.html#packages
* docker
* jq

## Versioning

`meta-resin` version is kept in `DISTRO_VERSION` variable. `resin-<board>` version is kept in the file called VERSION located in the root of the `resin-<board>` repository and read in the build as variable HOSTOS_VERSION.

* The version of `meta-resin` is in the format is 3 numbers separated by a dot. The patch number can have a `beta` label. e.g. 1.2.3, 1.2.3-beta1, 2.0.0-beta1.
* The version of `resin-<board>` is constructed by appending to the `meta-resin` version a `rev` label. This will have the semantics of a board revision which adapts a specific `meta-resin` version for a targeted board. For example a meta-resin 1.2.3 can go through 3 board revisions at the end of which the final version will be 1.2.3+rev3 .
* The first `resin-board` release based on a specific `meta-resin` release X.Y.Z, will be X.Y.Z+rev1 . Example: the first `resin-board` version based on `meta-resin` 1.2.3 will be 1.2.3+rev1 .
* When updating `meta-resin` version in `resin-board`, the revision will reset to 1. Ex: 1.2.3+rev4 will be updated to 1.2.4+rev1 .
* Note that the final OS version is NOT based on semver specification so parsing of such a version needs to be handled in a custom way.
* e.g. For `meta-resin` release 1.2.3 there can be `resin-<board>` releases 1.2.3+rev`X`.
* e.g. For `meta-resin` release 2.0.0-beta0 there can be `resin-<board>` releases 2.0.0-beta0+rev`X`.

We define host OS version as the `resin-<board>` version and we use this version as HOSTOS_VERSION.

## Build flags

Before bitbake-ing with meta-resin support, a few flags can be changed in the conf/local.conf from the build directory.
Editing of local.conf is to be done after source-ing.
See below for explanation on such build flags.

### Development Images

The DEVELOPMENT_IMAGE variable gets injected into DISTRO_FEATURES. If DEVELOPMENT_IMAGE = "1" then 'development-image' distro feature is added.
Based on this, recipes can decide what development specific changes are needed. By default DEVELOPMENT_IMAGE = "0" which corresponds to a normal (non-development) build (development-image won't be appended to DISTRO_FEATURE).
If user wants a build which creates development images (to use the serial console for example), DEVELOPMENT_IMAGE = "1" needs to be added to local.conf.

To make it short:

* If DEVELOPMENT_IMAGE is not present in your local.conf or it is not "1" : Non-development images will be generated (default behavior)
* If DEVELOPMENT_IMAGE is defined local.conf and its value is "1" : Development images will be generated

### Generation of host OS update bundles

In order to generate update resin host OS bundles, edit the build's local.conf adding:

RESINHUP = "yes"

### Configure custom network manager

By default resin uses NetworkManager on host OS to provide connectivity. If you want to change and use other providers, list your packages using NETWORK_MANAGER_PACKAGES. You can add this variable to local.conf. Here is an example:

NETWORK_MANAGER_PACKAGES = "mynetworkmanager mynetworkmanager-client"

### Customizing splash

We configure all of our initial images to produce a resin logo at boot, shutdown or reboot. But we encourage any user to go and replace that logo with their own.
All you have to do is replace the splash/resin-logo.png file that you will find in the first partition of our images (boot partition) with your own image.
NOTE: As it currently stands plymouth expects the image to be named resin-logo.png.

### Docker storage driver

By the default the build system will set all the bits needed for the docker to be able to use the `aufs` storage driver. This can be changed by defining `BALENA_STORAGE` in your local.conf. It supports `aufs` and `overlay2`.

## The OS

### SSH and Avahi services

The OS runs SSH (dropbear) on port 22222. Running this service takes advantage of the socket activation systemd feature so dropbear will only run when there is a SSH connection to the device saving idle resources in this way. In order to connect to a device, one can use it's IP when known or resolve the hostname over mDNS as its hostname is advertised over network using an avahi service. When the latter is used, configuration of the client is needed (see for example https://wiki.archlinux.org/index.php/Avahi#Hostname_resolution). 

### Time in the OS

We currently have three time sources:

* build time - stored in `/etc/timestamp` and generated by the build system when the image is generated
* network time - managed by chronyd
* RTC time when available

Early in the boot process, the OS will start three services associated with the sources listed above, which manage the system clock.

The first one is `timeinit-rtc`. This service, when a RTC is available (`/etc/rtc`) will update the system clock using the value read from the RTC. If there is no RTC available, the service will not do anything. The second service is `timeinit-timestamp` which reads the build timestamp and updates the system clock if the timestamp is after the current system clock. The third service is chronyd.service which is responsible of managing the time afterwards over NTP.

The order of the services is as stated above and provides a robust time initialization at boot in both cases where RTC is or not available.

## Devices support

### WiFi Adapters

We currently tested and provide explicit support for the following WiFi adapters:

* bcm43143 based adapters
    * Example: Official RPI WiFi adapter [link](http://thepihut.com/collections/new-products/products/official-raspberry-pi-wifi-adapter)

### Modems

We currently test as part of our release process and provide explicit support for the following modems:

* USB modems (tested on Raspberry Pi 3, Balena Fin, Intel NUC and Nvidia TX2)
  * Huawei MS2131i-8
  * Huawei MS2372
* mPCI modems (tested on Balena Fin and Nvidia TX2 Spacely carrier)
  * Huawei ME909s-120
  * Quectel EC20
  * SIM7600E

## How to fix various build errors

* Supervisor fails with a log similar to:
```
Step 3 : RUN chmod 700 /entry.sh
---> Running in 445fe69866f9
operation not supported
```
This is probably because of a docker bug where, if you update kernel and don't reboot, docker gets confused. The fix is to reboot your system.
More info: http://stackoverflow.com/questions/29546388/getting-an-operation-not-supported-error-when-trying-to-run-something-while-bu

## config.json

The behaviour of resinOS can be configured by setting the following keys in the config.json file in the boot partition. This configuration file is also used by the supervisor.

### hostname

String. The configured hostname of this device, otherwise the UUID is used.

### persistentLogging

Boolean. Enable or disable persistent logging on this device.

### country

String. The country in which the device is operating. This is used for setting with WiFi regulatory domain.

### ntpServers

String. A space-separated list of NTP servers to use for time synchronization. Defaults to resinio.pool.ntp.org servers.

### dnsServers

String. A space-separated list of preferred DNS servers to use for name resolution. Falls back to DHCP provided servers and Google DNS.

### os

Multiple settings that customize the OS at runtime are nested under here.

#### udevRules

String. Custom udev rules can be passed via config.json.

To turn a rule into the format that can be easily added to config.json, use

`cat rulefilename | jq -sR .`
e.g.
```
root@resin:/etc/udev/rules.d# cat 64.rules | jq -sR .
"ACTION!=\"add|change\", GOTO=\"modeswitch_rules_end\"\nKERNEL==\"ttyACM*\", ATTRS{idVendor}==\"1546\", ATTRS{idProduct}==\"1146\", TAG+=\"systemd\", ENV{SYSTEMD_WANTS}=\"u-blox-switch@'%E{DEVNAME}'.service\"\nLBEL=\"modeswitch_rules_end\"\n"
root@resin:/etc/udev/rules.d#
```

An example config.json snippet with 2 rules:

```
  "os": {
    "udevRules": {
      "56": "ENV{ID_FS_LABEL_ENC}==\"resin-root*\", IMPORT{program}=\"resin_update_state_probe $devnode\", SYMLINK+=\"disk/by-state/$env{RESIN_UPDATE_STATE}\"",
      "64" : "ACTION!=\"add|change\", GOTO=\"modeswitch_rules_end\"\nKERNEL==\"ttyACM*\", ATTRS{idVendor}==\"1546\", ATTRS{idProduct}==\"1146\", TAG+=\"systemd\", ENV{SYSTEMD_WANTS}=\"u-blox-switch@'%E{DEVNAME}'.service\"\nLBEL=\"modeswitch_rules_end\"\n"
   }
 }
```

This will create `/etc/udev/rules.d/99.rules` and `/etc/udev/rules.d/60.rules`
The first time rules are added/modified, these rules will be added and udevd will be asked to reload rules and re-trigger.

#### sshKeys

Array of strings. Holds a list of public SSH keys that will be used by the SSH server for authentication.

Example:
```
  "os": {
    "sshKeys": [
      "KEY1",
      "KEY2"
    ]
  }

```

## Yocto version support

The following Yocto versions are supported:
 * Sumo (2.5)
  * **TESTED**
 * Rocko (2.4)
  * **TESTED**
 * Pyro (2.3)
  * **TESTED**
 * Morty (2.2)
  * **TESTED**
 * Krogoth (2.1)
  * **TESTED**
