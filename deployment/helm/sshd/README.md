# Deployment Guilde

- [Dependencies](#dependencies)
- [Helm](#helm-repository)


## Dependencies

- An existing Kubernetes cluster, version 1.25 or greater.
- A working [`base` Helm Chart](https://github.com/opencadc/science-platform/tree/main/deployment/helm#base-install) install.
- A Kubernetes secret called `sssd-ldap-secret` in the Skaha Namespace (defaults to `skaha-system`) with a single key of `ldap-password` whose value is the password of the LDAP bind user as configured in the `values.yaml` file:
  - `kubectl -n skaha-system create secret generic sssd-ldap-secret --from-literal="ldap-password=my-super-secret-passwd"`
