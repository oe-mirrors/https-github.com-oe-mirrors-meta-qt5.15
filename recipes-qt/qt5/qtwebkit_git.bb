require qt5.inc
require qt5-git.inc

LICENSE = "BSD-3-Clause & LGPL-2.0-or-later | GPL-2.0-only"
LIC_FILES_CHKSUM = " \
    file://LICENSE.LGPLv21;md5=58a180e1cf84c756c29f782b3a485c29 \
    file://Source/JavaScriptCore/parser/Parser.h;endline=21;md5=bd69f72183a7af673863f057576e21ee \
"

DEPENDS += "qtbase qtdeclarative icu ruby-native sqlite3 glib-2.0 libxslt gperf-native bison-native flex-native"

# Patches from https://github.com/meta-qt5/qtwebkit/commits/b5.13
# 5.13.meta-qt5.1
SRC_URI += "\
    file://0001-Do-not-skip-build-for-cross-compile.patch \
    file://0002-Fix-build-with-non-glibc-libc-on-musl.patch \
    file://0004-PlatformQt.cmake-Do-not-generate-hardcoded-include-p.patch \
    file://0008-Fix-build-with-icu-68.patch \
    file://mips-atomic.patch \
    file://disable_qttestbrowser.patch \
"

inherit cmake_qt5 perlnative pkgconfig

inherit python3native

# qemuarm build fails with:
# | {standard input}: Assembler messages:
# | {standard input}:106: Error: invalid immediate: 983040 is out of range
# | {standard input}:106: Error: value of 983040 too large for field of 2 bytes at 146
ARM_INSTRUCTION_SET:armv4 = "arm"
ARM_INSTRUCTION_SET:armv5 = "arm"

# https://bugzilla.yoctoproject.org/show_bug.cgi?id=9474
# https://bugs.webkit.org/show_bug.cgi?id=159880
# JSC JIT can build on ARMv7 with -marm, but doesn't work on runtime.
# Upstream only tests regularly the JSC JIT on ARMv7 with Thumb2 (-mthumb).
ARM_INSTRUCTION_SET:armv7a = "thumb"
ARM_INSTRUCTION_SET:armv7r = "thumb"
ARM_INSTRUCTION_SET:armv7ve = "thumb"

# http://errors.yoctoproject.org/Errors/Details/179245/
# just use -fpermissive in this case like fedora did:
# https://bugzilla.redhat.com/show_bug.cgi?id=1582954
CXXFLAGS += "-fpermissive"
CXXFLAGS += "${@bb.utils.contains('DISTRO_FEATURES', 'x11', '', '-DEGL_NO_X11=1', d)}"

EXTRA_OECMAKE += " \
    -DPORT=Qt \
    -DCROSS_COMPILE=ON \
    -DCMAKE_BUILD_TYPE=release \
    -DECM_MKSPECS_INSTALL_DIR=${libdir}${QT_DIR_NAME}/mkspecs/modules \
    -DQML_INSTALL_DIR=${OE_QMAKE_PATH_QML} \
    -DPYTHON_EXECUTABLE=`which python3` \
"

EXTRA_OECMAKE:append:toolchain-clang = " -DCMAKE_CXX_IMPLICIT_INCLUDE_DIRECTORIES:PATH='${STAGING_INCDIR}'"

# JIT not supported on MIPS/PPC/RISCV
EXTRA_OECMAKE:append:mipsarch = " -DENABLE_JIT=OFF -DENABLE_C_LOOP=ON "
EXTRA_OECMAKE:append:powerpc = " -DENABLE_JIT=OFF -DENABLE_C_LOOP=ON "
EXTRA_OECMAKE:append:riscv64 = " -DENABLE_JIT=OFF -DENABLE_C_LOOP=ON "
EXTRA_OECMAKE:append:arm = " -DENABLE_JIT=OFF -DENABLE_C_LOOP=ON "

# Disable gold on mips64/clang
# mips64-yoe-linux-musl-ld.gold: internal error in get_got_page_offset, at ../../gold/mips.cc:6260
# mips-yoe-linux-musl-ld.gold: error: Can't find matching LO16 reloc
EXTRA_OECMAKE:append:mipsarch = " -DUSE_LD_GOLD=OFF "
EXTRA_OECMAKE:append:powerpc = " -DUSE_LD_GOLD=OFF "
EXTRA_OECMAKE:append:riscv64 = " -DUSE_LD_GOLD=OFF "

PACKAGECONFIG ??= "qtlocation qtmultimedia qtsensors qtwebchannel \
    ${@bb.utils.filter('DISTRO_FEATURES', 'x11', d)} \
    fontconfig \
"

# gstreamer conflicts with qtmultimedia!
PACKAGECONFIG[gstreamer] = "-DUSE_GSTREAMER=ON,-DUSE_GSTREAMER=OFF,gstreamer1.0 gstreamer1.0-plugins-base"
PACKAGECONFIG[qtlocation] = "-DENABLE_GEOLOCATION=ON,-DENABLE_GEOLOCATION=OFF,qtlocation"
PACKAGECONFIG[qtmultimedia] = "-DUSE_QT_MULTIMEDIA=ON,-DUSE_QT_MULTIMEDIA=OFF,qtmultimedia"
PACKAGECONFIG[qtsensors] = "-DENABLE_DEVICE_ORIENTATION=ON,-DENABLE_DEVICE_ORIENTATION=OFF,qtsensors"
PACKAGECONFIG[qtwebchannel] = "-DENABLE_QT_WEBCHANNEL=ON,-DENABLE_QT_WEBCHANNEL=OFF,qtwebchannel"
PACKAGECONFIG[libwebp] = ",,libwebp"
PACKAGECONFIG[x11] = "-DENABLE_X11_TARGET=ON,-DENABLE_X11_TARGET=OFF,libxcomposite libxrender"
PACKAGECONFIG[fontconfig] = "-DENABLE_TEST_SUPPORT=ON,-DENABLE_TEST_SUPPORT=OFF,fontconfig"
# hyphen is only in meta-office currently!
PACKAGECONFIG[hyphen] = "-DUSE_LIBHYPHEN=ON,-DUSE_LIBHYPHEN=OFF,hyphen"

# remove default ${PN}-examples* set in qt5.inc, because they conflicts with ${PN} from separate webkit-examples recipe
PACKAGES:remove = "${PN}-examples"

QT_MODULE_BRANCH = "5.212"

SRCREV = "ac8ebc6c3a56064f88f5506e5e3783ab7bee2456"
