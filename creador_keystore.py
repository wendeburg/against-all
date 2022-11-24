import os
import random
import sys
import subprocess

module_name = sys.argv[1]
ks_num_start = int(sys.argv[2])
ks_num_end = int(sys.argv[3])
ks_pass = sys.argv[4]
ca_cert_path = sys.argv[5]
ca_key_path = sys.argv[6]
srl_file_path = sys.argv[7]

dst_path = f"./{module_name}_secrets/"

os.mkdir(dst_path)

for i in range(ks_num_start, ks_num_end):
    os.system(f"keytool -genkey -noprompt \
    -alias {module_name}-{i} \
    -dname CN='{module_name}-{i}' \
    -keystore {dst_path}{module_name}.{i}.keystore.jks \
    -keyalg RSA \
    -storepass {ks_pass} \
    -keypass {ks_pass}")

    os.system(f"keytool -keystore {dst_path}{module_name}.{i}.keystore.jks -alias {module_name}-{i} -certreq -file {dst_path}{module_name}-{i}.csr -storepass {ks_pass} -keypass {ks_pass}")

    os.system(f"openssl x509 -req -CA {ca_cert_path} -CAkey {ca_key_path} -in {dst_path}{module_name}-{i}.csr -out {dst_path}{module_name}-{i}-signed.csr -days 9999 -CAserial {srl_file_path} -passin pass:against-all-ca-password")

    os.system(f"keytool -keystore {dst_path}{module_name}.{i}.keystore.jks -alias CARoot -import -file {ca_cert_path} -storepass {ks_pass} -keypass {ks_pass}")

    os.system(f"keytool -keystore {dst_path}{module_name}.{i}.keystore.jks -alias {module_name}-{i} -import -file {dst_path}{module_name}-{i}-signed.csr -storepass {ks_pass} -keypass {ks_pass}")

os.system(f"echo {ks_pass} > {dst_path}{module_name}_creds")