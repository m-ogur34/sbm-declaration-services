{{- define "allianz.bundlename" -}}
{{- if .Values.global.overrides.bundleName }}
{{- .Values.global.overrides.bundleName }}
{{- else }}
{{- .Values.global.bundleName }}
{{- end }}
{{- end }}

{{- define "isArrayEmpty" -}}
  {{- $empty := true -}}
  {{- range .list }}
    {{- $empty = false -}}
  {{- end }}
  {{- $empty -}}
{{- end }}
