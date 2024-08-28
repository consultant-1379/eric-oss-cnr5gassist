{{/* vim: set filetype=mustache: */}}

{{/*
Create a map from ".Values.global" with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{ define "eric-oss-5gcnr.global" }}
  {{- $globalDefaults := get (fromYaml (.Files.Get "resources/global_defaults.yaml")) "global" }}
  {{ if .Values.global }}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{ else }}
    {{- $globalDefaults | toJson -}}
  {{ end }}
{{ end }}

{{/*
Assemble URL for a specific global host
*/}}
{{- define "eric-oss-5gcnr.global.hostUrl" }}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
  {{- $url := dict "scheme" (index $global "schemes" .host) "host" (index $global "hosts" .host) }}
  {{- urlJoin $url -}}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-5gcnr.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-5gcnr.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-5gcnr.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}


{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-5gcnr.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-5gcnr.pullSecret" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) -}}
  {{- if .Values.imageCredentials -}}
    {{- if .Values.imageCredentials.pullSecret -}}
      {{- $_ := set $global "pullSecret" .Values.imageCredentials.pullSecret -}}
    {{- end -}}
  {{- end -}}
  {{- print $global.pullSecret -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-5gcnr.timezone" }}
{{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
{{- print $global.timezone | quote }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-oss-5gcnr.standard-labels" -}}
helm.sh/chart: {{ include "eric-oss-5gcnr.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{ include "eric-oss-5gcnr.selectorLabels" . }}
app.kubernetes.io/version: {{ include "eric-oss-5gcnr.version" . }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-5gcnr.fsGroup.coordinated" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
  {{- $fsGroup := dict "fsGroup" 10000 }}
  {{- if $global.fsGroup.manual -}}
    {{- $_ := set $fsGroup "fsGroup" $global.fsGroup.manual }}
  {{- else -}}
    {{- if eq $global.fsGroup.namespace true -}}
      {{- $_ := unset $fsGroup "fsGroup" }}
    {{- end -}}
  {{- end -}}
{{- range $name, $config := $fsGroup }}
{{ $name }}: {{ $config }}
{{- end }}
{{- end -}}


{{/*
Return the supplementalGroup set via global parameter if it's set, else use local configuration
*/}}
{{- define "eric-oss-5gcnr.supplementalGroups" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
  {{- $supplementalGroups := dict }}
  {{- if $global.podSecurityContext -}}
    {{- if $global.podSecurityContext.supplementalGroups -}}
        {{- $_ := set $supplementalGroups "supplementalGroups" $global.podSecurityContext.supplementalGroups }}
    {{- end -}}
  {{- end -}}
  {{- if .Values.podSecurityContext -}}
    {{- if .Values.podSecurityContext.supplementalGroups -}}
        {{- $_ := set $supplementalGroups "supplementalGroups" .Values.podSecurityContext.supplementalGroups }}
    {{- end -}}
  {{- end -}}
{{- range $name, $config := $supplementalGroups }}
{{ $name }}: {{ $config }}
{{- end }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "eric-oss-5gcnr.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-5gcnr.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Get peer services labels (DR-D1125-056-AD)
*/}}
{{- define "eric-oss-5gcnr.peerServiceAccess.labels"}}
{{ .Values.pmServer.name }}-access: "true"
{{ .Values.apiGateway.name }}-access: "true"
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-5gcnr.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-5gcnr.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Create a user defined annotation (DR-D1121-065)
*/}}
{{- define "eric-oss-5gcnr.config-annotations" }}
{{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
{{- $global_annotations := $global.annotations -}}
{{- $custom_annotations := .Values.annotations -}}
{{- include "eric-oss-5gcnr.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global_annotations $custom_annotations)) -}}
{{- end }}


{{/*
Create a user defined labels (DR-D1121-068)
*/}}
{{ define "eric-oss-5gcnr.config-labels" }}
{{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
{{- $global_labels := $global.labels  -}}
{{- $custom_labels := .Values.labels  -}}
{{- include "eric-oss-5gcnr.mergeLabels" (dict "location" .Template.Name "sources" (list $global_labels $custom_labels)) -}}
{{- end }}

{{- define "eric-oss-5gcnr.labels" -}}
  {{- $standard := include "eric-oss-5gcnr.standard-labels" . | fromYaml -}}
  {{- $config := include "eric-oss-5gcnr.config-labels" . | fromYaml -}}
  {{- include "eric-oss-5gcnr.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $config)) }}
{{- end -}}

{{/*
Create product info.
*/}}
{{- define "eric-oss-5gcnr.product-info" }}
{{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") }}
ericsson.com/product-name: {{ $productInfo.productName | quote }}
ericsson.com/product-number: {{ $productInfo.productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create prometheus info
*/}}
{{- define "eric-oss-5gcnr.prometheus" }}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
{{- end }}

{{/*
Create log control configmap name.
*/}}
{{- define "eric-oss-5gcnr.log-control-configmap.name" }}
  {{- include "eric-oss-5gcnr.name.noQuote" . | printf "%s-log-control-configmap" | quote }}
{{- end }}

{{/*
Merged annotations for Default, which includes productInfo and config
*/}}
{{- define "eric-oss-5gcnr.annotations" -}}
  {{- $productInfo := include "eric-oss-5gcnr.product-info" . | fromYaml -}}
  {{- $config := include "eric-oss-5gcnr.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-5gcnr.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) }}
{{- end -}}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-5gcnr.securityPolicy.reference" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
  {{- $reference := "default-restricted-security-policy" }}
  {{- if $global.security.policyReferenceMap -}}
    {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
    {{- if $mapped -}}
        {{ $reference = $mapped }}
    {{- end -}}
  {{- end -}}
  {{- print $reference }}
{{- end -}}

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-5gcnr.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-5gcnr.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-5gcnr.nodeSelector" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) -}}
  {{- if .Values.nodeSelector }}
    {{- toYaml (mergeOverwrite $global.nodeSelector .Values.nodeSelector) }}
  {{- else }}
    {{- toYaml $global.nodeSelector }}
  {{- end }}
{{- end -}}

{{/*
Support dual stack for ClusterIP typed services (DR-D1125-018)
*/}}
{{- define "eric-oss-5gcnr.internalIPFamily" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
  {{- print $global.internalIPFamily -}}
{{- end -}}

{{- define "eric-oss-5gcnr.affinity" -}}
{{- if (eq .Values.affinity.podAntiAffinity "hard") -}}
podAntiAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
      matchExpressions:
        - key: app
          operator: In
          values:
            - {{ .name }}
      topologyKey: "kubernetes.io/hostname"
{{- else if (eq .Values.affinity.podAntiAffinity "soft") -}}
podAntiAffinity:
  preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
            - key: app
              operator: In
              values:
                - {{ .name }}
        topologyKey: "kubernetes.io/hostname"
{{- end -}}
{{- end -}}

{{/*
The image path (DR-D1121-067)(DR-D1121-106)
*/}}
{{- define "eric-oss-5gcnr.image" -}}
  {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
  {{- $productInfo = index $productInfo "images" .imageName -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) -}}
  {{- if $global.registry -}}
      {{- if $global.registry.url -}}
        {{- $_ := set $productInfo "registry" $global.registry.url -}}
      {{- end -}}
      {{- if not (kindIs "invalid" $global.registry.repoPath) -}}
        {{- $_ := set $productInfo "repoPath" ($global.registry.repoPath) -}}
      {{- end -}}
  {{- end -}}
  {{- if .Values.imageCredentials -}}
    {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
      {{- $_ := set $productInfo "repoPath" (index .Values "imageCredentials" "repoPath") }}
    {{- end -}}
    {{- if hasKey .Values.imageCredentials .imageName -}}
        {{- if hasKey (index .Values "imageCredentials" .imageName) "registry" }}
          {{- if hasKey (index .Values "imageCredentials" .imageName "registry") "url" }}
            {{- $_ := set $productInfo "registry" (index .Values "imageCredentials" .imageName "registry" "url") }}
          {{- end }}
        {{- end }}
        {{- if not (kindIs "invalid" (index .Values "imageCredentials" .imageName "repoPath") ) -}}
          {{- $_ := set $productInfo "repoPath" (index .Values "imageCredentials" .imageName "repoPath") }}
        {{- end -}}
    {{- end }}
  {{- end -}}
  {{- if hasKey (index .Values "imageCredentials" .imageName) "repoPath" -}}
    {{- if index .Values "imageCredentials" .imageName "repoPath" -}}
      {{- $_ := set $productInfo "repoPath" (index .Values "imageCredentials" .imageName "repoPath") -}}
    {{- end -}}
  {{- end -}}
  {{- if hasKey (index .Values "images" .imageName) "name" -}}
    {{- $_ := set $productInfo "name" (index .Values "images" .imageName "name") -}}
  {{- end -}}
  {{- if hasKey (index .Values "images" .imageName) "tag" -}}
    {{- $_ := set $productInfo "tag" (index .Values "images" .imageName "tag") -}}
  {{- end -}}
  {{- if $productInfo.repoPath -}}
      {{- $_ := set $productInfo "repoPath" (printf "%s/" $productInfo.repoPath) -}}
  {{- end -}}
  {{- printf "%s/%s%s:%s" $productInfo.registry $productInfo.repoPath $productInfo.name $productInfo.tag -}}
{{- end -}}

{{/*
The image pullPolicy (DR-D1121-102)
*/}}
{{- define "eric-oss-5gcnr.imagePullPolicy" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) -}}
  {{- $pullPolicy := default $global.registry.imagePullPolicy (get (index .Values "imageCredentials" .imageName) "pullPolicy") }}
  {{- print $pullPolicy | quote }}
{{- end -}}

{{/*
Register Hooks Condition
*/}}
{{- define "eric-oss-5gcnr.hook.register" -}}
  {{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
  {{- $condition := not (empty (and .Values.integration.policyFramework.credentials.login .Values.integration.policyFramework.credentials.password)) }}
  {{- $condition = or $condition (not (empty .Values.integration.policyFramework.secretName)) }}
  {{- $condition = and $condition .Values.integration.policyFramework.hooks.enabled }}
  {{- $condition = and $condition (not (empty (and $global.hosts.iam $global.hosts.pf))) }}
  {{- print $condition }}
{{- end -}}

{{/*
The Liveness Probe (DR-D1120-012)
*/}}
{{- define "eric-oss-5gcnr.livenessProbe" -}}
{{- with (index .Values "probes" "eric-oss-5gassist" "livenessProbe") }}
  {{- toYaml . }}
{{- end }}
{{- end -}}

{{/*
The Readiness Probe (DR-D1120-014)
*/}}
{{- define "eric-oss-5gcnr.readinessProbe" -}}
{{- with (index .Values "probes" "eric-oss-5gassist" "readinessProbe") }}
  {{- toYaml . }}
{{- end }}
{{- end -}}

{{- define "eric-oss-5gcnr.priorityClassName" -}}
{{- if index .Values "podPriority" "eric-oss-5gassist" "priorityClassName" }}
priorityClassName: {{ index .Values "podPriority" "eric-oss-5gassist" "priorityClassName" | quote }}
{{- end }}
{{- end -}}

{{/*-------------------ServiceMesh related changes------------------------------*/}}

{{/*
check global.security.tls.enabled
*/}}
{{- define "eric-oss-5gcnr.global-security-tls-enabled" -}}
{{- if  .Values.global -}}
  {{- if  .Values.global.security -}}
    {{- if  .Values.global.security.tls -}}
       {{- .Values.global.security.tls.enabled | toString -}}
    {{- else -}}
       {{- "false" -}}
    {{- end -}}
  {{- else -}}
       {{- "false" -}}
  {{- end -}}
{{- else -}}
{{- "false" -}}
{{- end -}}
{{- end -}}

{{/*
DR-D470217-007-AD This helper defines whether this service enter the Service Mesh or not.
*/}}
{{- define "eric-oss-5gcnr.service-mesh-enabled" }}
  {{- $globalMeshEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
        {{- $globalMeshEnabled = .Values.global.serviceMesh.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEnabled -}}
{{- end -}}


{{/*
DR-D470217-011 This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-oss-5gcnr.service-mesh-inject" }}
{{- if eq (include "eric-oss-5gcnr.service-mesh-enabled" .) "true" }}
sidecar.istio.io/inject: "true"
{{- else -}}
sidecar.istio.io/inject: "false"
{{- end -}}
{{- end -}}

{{/*
GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-oss-5gcnr.service-mesh-version" }}
{{- if eq (include "eric-oss-5gcnr.service-mesh-enabled" .) "true" }}
  {{- if .Values.global.serviceMesh -}}
    {{- if .Values.global.serviceMesh.annotations -}}
      {{ .Values.global.serviceMesh.annotations | toYaml }}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
This helper defines the annotation which bring the hook Job into the mesh.
*/}}
{{- define "eric-oss-5gcnr.service-mesh-hook-annotations" -}}
{{- $serviceMesh := ( include "eric-oss-5gcnr.service-mesh-enabled" . ) -}}
{{- if (eq $serviceMesh "true") -}}
sidecar.istio.io/inject: "false"
proxy.istio.io/config: {{ .Values.serviceMesh.proxy | quote }}
{{- else -}}
sidecar.istio.io/inject: "false"
{{- end -}}
{{- end -}}

{{/*
This helper defines the annotation for define service mesh volume
*/}}
{{- define "eric-oss-5gcnr.service-mesh-volume" }}
{{- if and (eq (include "eric-oss-5gcnr.service-mesh-enabled" .) "true") (eq (include "eric-oss-5gcnr.global-security-tls-enabled" .) "true") }}
sidecar.istio.io/userVolume: '{"eric-oss-5gcnr-certs-tls":{"secret":{"secretName":"eric-oss-5gcnr-secret","optional":true}},"eric-oss-5gcnr-certs-ca-tls":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert"}}}'
sidecar.istio.io/userVolumeMount: '{"eric-oss-5gcnr-certs-tls":{"mountPath":"/etc/istio/tls/","readOnly":true},"eric-oss-5gcnr-certs-ca-tls":{"mountPath":"/etc/istio/tls-ca","readOnly":true}}'
{{ end }}
{{- end -}}

{{/*------------------------------------------------------------------------------*/}}

{{/*
Define the log streaming method (DR-470222-010)
*/}}
{{- define "eric-oss-5gcnr.streamingMethod" -}}
{{- $streamingMethod := "direct" -}}
{{- $global := fromJson (include "eric-oss-5gcnr.global" .) }}
{{- if $global -}}
  {{- if $global.log -}}
      {{- if $global.log.streamingMethod -}}
        {{- $streamingMethod = $global.log.streamingMethod }}
      {{- end -}}
  {{- end -}}
{{- end -}}
{{- if .Values.log -}}
  {{- if .Values.log.streamingMethod -}}
    {{- $streamingMethod = .Values.log.streamingMethod }}
  {{- end -}}
{{- end -}}
{{- print $streamingMethod -}}
{{- end -}}

{{/*
Define the label needed for reaching eric-log-transformer (DR-470222-010)
*/}}
{{- define "eric-oss-5gcnr.directStreamingLabel" -}}
{{- $streamingMethod := (include "eric-oss-5gcnr.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) }}
logger-communication-type: "direct"
{{- end -}}
{{- end -}}

{{/*
Define logging environment variables (DR-470222-010)
*/}}
{{ define "eric-oss-5gcnr.loggingEnv" }}
{{- $streamingMethod := (include "eric-oss-5gcnr.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) -}}
  {{- if eq "direct" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-http.xml"
  {{- end }}
  {{- if eq "dual" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-dual.xml"
  {{- end }}
- name: LOGSTASH_DESTINATION
  value: eric-log-transformer
- name: LOGSTASH_PORT
  value: "9080"
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_UID
  valueFrom:
    fieldRef:
      fieldPath: metadata.uid
- name: CONTAINER_NAME
  value: eric-oss-5gcnr
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
- name: NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
{{- else if eq $streamingMethod "indirect" }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-json.xml"
{{- else }}
  {{- fail ".log.streamingMethod unknown" }}
{{- end -}}
{{ end }}
