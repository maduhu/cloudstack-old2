#!/usr/bin/python
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


# Usage: prepare_tftp_bootfile.py tftp_dir mac cifs_server share directory image_to_restore cifs_username cifs_password
import os, sys
from sys import exit
from os import makedirs
from os.path import exists, join

restore_template = '''DEFAULT default
PROMPT 1
TIMEOUT 26
LABEL default
MENU default
KERNEL images/clonezilla/live/vmlinuz
APPEND initrd=images/clonezilla/live/initrd.img boot=live config noswap nolocales edd=on nomodeset ocs_live_run="/usr/sbin/ocs-sr -g auto -p reboot restoreparts %s all" ocs_live_extra_param="" ocs_live_keymap="NONE" ocs_live_batch="yes" ocs_lang="en_US.UTF-8" vga=788 nosplash noprompt ocs_prerun1="mdadm --zero-superblock --force /dev/sda1 && mdadm --zero-superblock --force /dev/sdb1" ocs_prerun2="mount -t nfs %s:%s /home/partimag" fetch=tftp://%s/images/clonezilla/live/filesystem.squashfs'''

backup_template = '''DEFAULT default
PROMPT 1
TIMEOUT 26
LABEL default
MENU default
KERNEL images/clonezilla/live/vmlinuz
APPEND initrd=images/clonezilla/live/initrd.img boot=live config noswap nolocales edd=on nomodeset ocs_live_run="/usr/sbin/ocs-sr -q2 -j2 -z1p -i 2000 -fsck-src-part-y -p reboot saveparts %s all" ocs_live_extra_param="" ocs_live_keymap="NONE" ocs_live_batch="yes" ocs_lang="en_US.UTF-8" vga=788 nosplash noprompt ocs_prerun1="mdadm --zero-superblock --force /dev/sda1 && mdadm --zero-superblock --force /dev/sdb1" ocs_prerun2="mount -t nfs %s:%s /home/partimag" fetch=tftp://%s/images/clonezilla/live/filesystem.squashfs'''

cmd = ''
tftp_dir = ''
mac = ''
cifs_server = ''
share = ''
directory = ''
template_dir = ''
cifs_username = ''
cifs_password = ''
ip = ''
netmask = ''
gateway = ''

def prepare(is_restore):
    try:
        pxelinux = join(tftp_dir, "pxelinux.cfg")
        if exists(pxelinux) == False:
            makedirs(pxelinux)

        cfg_name = "01-" + mac.replace(':','-').lower()
        cfg_path = join(pxelinux, cfg_name)
        f = open(cfg_path, "w")
        if is_restore:
            fmt = restore_template
        else:
            fmt = backup_template
        nfs_server=cifs_server
        tftp_server=cifs_server
        nfs_export="/home/shares/"+share+"/"+directory
        stuff = fmt % (template_dir, nfs_server, nfs_export, tftp_server)
        f.write(stuff)
        f.close()
        return 0
    except Exception, e:
        print e
        return 1


if __name__ == "__main__":
    if len(sys.argv) < 12:
        print "Usage: prepare_tftp_bootfile.py tftp_dir mac cifs_server share directory image_to_restor cifs_username cifs_password ip netmask gateway"
        exit(1)

    (cmd, tftp_dir, mac, cifs_server, share, directory, template_dir, cifs_username, cifs_password, ip, netmask, gateway) = sys.argv[1:]
    
    if cmd == "restore":
        ret = prepare(True)
    elif cmd == "backup":
        ret = prepare(False)
    else:
        print "Unknown cmd: %s"%cmd
        ret = 1
        
    exit(ret)
