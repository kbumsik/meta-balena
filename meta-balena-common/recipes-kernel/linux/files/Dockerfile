# Use stretch as we have devices with old 3.10 kernel that won't build with gcc8 in buster
FROM balenalib/intel-nuc-debian:stretch-20190717

# Install dependencies
RUN apt-get update && apt-get install -y curl wget build-essential libelf-dev bc flex libssl-dev bison gcc-arm-linux-gnueabi gcc-aarch64-linux-gnu

# Fetch kernel module headers built and source
ADD example_module /usr/src/app/example_module_source/
ADD example_module /usr/src/app/example_module_built/
ADD kernel_source.tar.gz /usr/src/app/kernel_headers_source/
ADD kernel_modules_headers.tar.gz /usr/src/app/kernel_headers_built/
WORKDIR /usr/src/app

# Workaround for a subtle issue in the linker path. Debian defaults to /lib64/ld-linux-x86-64.so.2 while balenaOS defaults to /lib/ld-linux-x86-64.so.2 for os/headers. Affects NUC.
RUN if [ -f /lib64/ld-linux-x86-64.so.2 ]; then \
	if [ ! -f /lib/ld-linux-x86-64.so.2 ]; then \
		ln -s /lib64/ld-linux-x86-64.so.2  /lib/ld-linux-x86-64.so.2 ; \
	fi; \
    fi;

# Compile external hello module using sources
RUN ARCH=@KERNEL_ARCH@ CROSS_COMPILE=@CROSS_COMPILE_PREFIX@ make -C /usr/src/app/kernel_headers_source/ modules_prepare
RUN ARCH=@KERNEL_ARCH@ CROSS_COMPILE=@CROSS_COMPILE_PREFIX@ KERNEL_TREE_PATH=/usr/src/app/kernel_headers_source/ MODULE_PATH=/usr/src/app/example_module_source  make -C /usr/src/app/example_module_source

# Compile external hello module using pre-built headers
# We run modules_prepare again because the tools pre-compiled are actually the target device arch. While we cross-compile for testing.
RUN ARCH=@KERNEL_ARCH@ CROSS_COMPILE=@CROSS_COMPILE_PREFIX@ make -C /usr/src/app/kernel_headers_built/*/ modules_prepare
RUN ARCH=@KERNEL_ARCH@ CROSS_COMPILE=@CROSS_COMPILE_PREFIX@ KERNEL_TREE_PATH=/usr/src/app/kernel_headers_built/*/ MODULE_PATH=/usr/src/app/example_module_built  make -C /usr/src/app/example_module_built
