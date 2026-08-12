{{/*
Bundle name. global.overrides.bundleName wins when set, otherwise global.bundleName.
Every resource this chart renders is named and labelled from it.
*/}}
{{- define "allianz.bundlename" -}}
{{- $overrides := .Values.global.overrides | default dict -}}
{{- default .Values.global.bundleName $overrides.bundleName | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Repository name, used where the artifact rather than the deployment is meant.
*/}}
{{- define "allianz.reponame" -}}
{{- .Values.global.repoName | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Active Spring profile, taken from the environment values file.
*/}}
{{- define "allianz.profile" -}}
{{- index .Values "springboot-deployment" "app" "env" "springboot" "profile" -}}
{{- end -}}
