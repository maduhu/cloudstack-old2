#!/usr/bin/env python
#
# Usage: prepare_basicpxe_bootfile.py <tftp_dir> <mac> <kernel> <append>
#

import sys
from base64 import b64decode
from sys import exit
from os import makedirs
from os.path import exists, join

tftp_dir = ''
mac = ''
kernel = ''
append = ''

pxe_template = '''DEFAULT default
PROMPT 1
TIMEOUT 5
LABEL default
MENU default
KERNEL %s
APPEND %s
'''


def prepare():
    try:
        pxelinux = join(tftp_dir, "pxelinux.cfg")
        if not exists(pxelinux):
            makedirs(pxelinux)

        cfg_name = "01-" + mac.replace(':', '-').lower()
        cfg_path = join(pxelinux, cfg_name)

        with open(cfg_path, "w") as f:
            contents = pxe_template % (b64decode(kernel), b64decode(append))
            f.write(contents)
        return 0

    except Exception, e:
        print e
        return 1


if __name__ == "__main__":
    if len(sys.argv) is not 5:
        print "Usage: prepare_basicpxe_bootfile.py tftp_dir mac kernel append"
        exit(1)

    (tftp_dir, mac, kernel, append) = sys.argv[1:]
    exit(prepare())
