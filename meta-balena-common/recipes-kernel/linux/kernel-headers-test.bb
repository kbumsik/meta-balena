SUMMARY = "Linux kernel headers test"
DESCRIPTION = "This recipe tests generated kernel headers by running a simple hello-world compile test"
SECTION = "devel/kernel"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://example_module/* \
           file://Dockerfile"

inherit kernel-arch

# Derived from kernel-arch.bbclass
valid_debian_archs = "i386 x86 arm aarch64"

def map_debian_arch(a, d):
    import re

    valid_debian_archs = d.getVar('valid_debian_archs').split()

    if   re.match('x86', a):        return 'x86_64-linux-gnu-'
    elif re.match('arm64', a):      return 'aarch64-linux-gnu-'
    elif re.match('arm', a):        return 'arm-linux-gnueabi-'
    else:
        bb.error("cannot map '%s' to a debian tuple" % a)

DEBIAN_ARCH ?= "${@map_debian_arch(d.getVar('ARCH'), d)}"

do_compile() {
    rm -rf ${B}/work
    mkdir -p ${B}/work
    cp ${DEPLOY_DIR_IMAGE}/kernel_source.tar.gz ${B}/work
    cp ${DEPLOY_DIR_IMAGE}/kernel_modules_headers.tar.gz ${B}/work
    cp "${WORKDIR}"/Dockerfile ${B}/work/
    cp "${WORKDIR}"/example_module ${B}/work/ -r
    CROSS_COMPILE_PREFIX=$(echo ${TARGET_PREFIX} | cut -d'-' -f1)
    CROSS_COMPILE_PREFIX=${CROSS_COMPILE_PREFIX}-
    sed -i "s/@KERNEL_ARCH@/${ARCH}/g" ${B}/work/Dockerfile
    sed -i "s/@CROSS_COMPILE_PREFIX@/${DEBIAN_ARCH}/g" ${B}/work/Dockerfile

    IMAGE_ID=$(DOCKER_API_VERSION=1.22 docker build ${B}/work | grep -o -E '[a-z0-9]{12}' | tail -n1)
    DOCKER_API_VERSION=1.22 docker rmi "$IMAGE_ID"
}

# Explicitly depend on the do_deploy step as we use the deployed artefacts. DEPENDS doesn't cover that
do_compile[depends] += "kernel-devsrc:do_deploy"
do_compile[depends] += "kernel-modules-headers:do_deploy"
