# Module enrollment

### Fingerprint Settings Enrollment Modules

This directory is responsible for containing the enrollment modules, each enrollment module is
responsible for the actual enrolling portion of FingerprintEnrollment.
The modules should be split out into udfps, rfps, and sfps.

[comment]: <>  This file structure print out has been generated with the tree command.

```
├── enrolling
│   └── rfps
│       ├── data
│       ├── domain
│       │   └── RFPSInteractor.kt
│       ├── README.md
│       └── ui
│           ├── fragment
│           │   └── RFPSEnrollFragment.kt
│           ├── viewmodel
│           │   └── RFPSViewModel.kt
│           └── widget
│               └── RFPSProgressIndicator.kt
└── README.md
```