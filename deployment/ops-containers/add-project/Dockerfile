FROM fedora:40 

# add often used tools
RUN dnf -y install which

# needed to get/set quota
RUN dnf -y install attr

# needed to set ACLs
RUN dnf -y install acl

# system settings and permissions
COPY src/add-project /usr/bin

CMD ["/usr/bin/add-project"]

