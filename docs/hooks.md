# Introduction

Host OS update hooks run on devices to configure the device before a Host OS Update. They usually handle the boot partition or any migrations between various OS versions.

resinHUP runs the `hostapp-update` script on device with the right flags that point to the new OS docker tag.

# Hooks v1.0 flow
- __v2.38 and before support only Hooks v1.0 flow__
- Hooks are scripts in the folder `/etc/hostapp-update-hooks.d/`
- `hostapp-update` downloads the new OS in the inactive partition
- Then the `hostapp-update-hooks` script of the new OS will be run inside a container of the new OS environment while mounting the `/mnt/` folder so that the new hooks can modify the boot partition easily.
- Hooks only run in the new OS environment

# Hooks v2.0 flow
Hooks v2.0 improve hooks v1.0 and allow trigging hooks from the new OS at various points in the host OS update process. Even in the old OS environment.
- __Supported v2.39 onwards__
- Hooks 2.0 will exist as a folder in `/etc/hostapp-update-hooks.d/`. The folder will contain several hooks that are related to each other.
- Folder name format follows the same format as hooks 1.0 i.e. XX-HOOKNAME where XX is a number to order the hooks priority.
Inside the folder
- _prehup_HOOKNAME_: This runs in previous OS environment.
 - e.g. Before hup, we’d like to save the vpn state for rollbacks

- _forward_HOOKNAME_: This is pretty much the same as Hooks 1.0
 - e.g. aufs to overlay migration hook. The part that makes the overlay metadata

- _forward_cleanup_HOOKNAME_: In case any HUP hook fails to run, the forward_cleanup_HOOKNAME is run.
 - e.g. Clean up overlay folder as aufs to overlay migration hasn’t worked out.

- _forward_commit_HOOKNAME_: This is run by rollbacks in the new OS in the new OS environment for any backwards incompatible steps.
 - This runs after rollbacks marks the OS as healthy
 - This is the destructive non-reversible part of the hooks.
 - e.g. Clean up aufs folder as aufs to overlay migration is complete
 - These are run in the new OS environment by rollbacks

- _posthup_HOOKNAME_: This runs in previous OS environment, but after the hooks 1.0 have run.

- Note: `pre` `post` `commit` `cleanup` are now keywords and HOOKNAME cannot contain those.

# Potential Issues/Cautionary statements
- If a power failure happens, next attempt at HUP will run hooks again, so hooks need to be idempotent.
- Existing OS will be running the previous version of `hostapp-update` up until the point when it downloads the new os and then it will run the new version of `hostapp-update-hooks` in a container in the new OS environment.
- __If a device is migrating from an OS version before v2.39, prehup and posthup hooks wont run!__
 Failures and the cleanup will run the previous version of `hostapp-update-hooks`

# Possible Future Extensions
Currently we aren’t writing hooks that properly ‘roll’ things back. i.e. forward hooks should be complemented by proper backward hooks so that a proper roll back can happen.
That can be added in the future.
