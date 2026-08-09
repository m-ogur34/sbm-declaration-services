{{/*
Chart name, truncated to the 63 character limit Kubernetes imposes on names.
*/}}
{{- define "sbm-declaration-services.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Fully qualified application name.
*/}}
{{- define "sbm-declaration-services.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s" (include "sbm-declaration-services.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Active Spring profile, taken from the environment values file.
*/}}
{{- define "sbm-declaration-services.profile" -}}
{{- index .Values "springboot-deployment" "app" "env" "springboot" "profile" -}}
{{- end -}}

{{/*
Standard labels.
*/}}
{{- define "sbm-declaration-services.labels" -}}
app.kubernetes.io/name: {{ include "sbm-declaration-services.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
{{- end -}}
