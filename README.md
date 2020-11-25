# Clarin Curation Module

The goal of this project is to implement software component 
for curation and quality assessment which can be 
integrated in the CLARINs VLO workflow. 
Project is initialized by Metadata Curation 
Task Force. Specification for the Curation 
Module is based on the Metadata Quality 
Assessement Service proposal. Curation 
Module validates and normalizes single MD r
ecords, repositories and profiles, to assess
their quality and to produce reports with 
different information for different actors
in VLO workflow. For implementation this 
project will use some of the existing CLARIN components. 

### Architecture
![Architecture](curation-module-general-architecture.png)

### Curation Module Core
usable as stand-alone application to generate instance/collection reports and as required API in the curation web module

### Curation Module Web
deployable web application

### Stormychecker
stand-alone application for permanent checking of http links

