## FS-MUAEKS

This is the category of the proposed FS-MUAEKS cryptographic scheme. 

### ``FS-MUAEKS.py``

This is the official implementation of the FS-MUAEKS cryptographic scheme in Python programming language. 

```bibtex
@article{xu2025lattice,
  title={Lattice-based forward secure multi-user authenticated searchable encryption for cloud storage systems},
  author={Xu, Shiyuan and Chen, Xue and Guo, Yu and Yang, Yuer and Wang, Shengling and Yiu, Siu-Ming and Cheng, Xiuzhen},
  journal={IEEE Transactions on Computers},
  year={2025},
  publisher={IEEE}
}
```

We would like to express our sincere gratitude to everyone who has cited this work. Readers who wish to cite this work may use the BibTeX entry above. 

#### Option

- [/n|-n|n]: Specify that the following option is the value of $n$ (default: $256$). 
- [/m|-m|m]: Specify that the following option is the value of $m$ (default: $4096$). 
- [/q|-q|q]: Specify that the following option is the value of $q$ (default: $256$). 
- [/ls|--ls|ls|/l_s|--l_s|l_s]: Specify that the following option is the value of $l_S$ (default: $32$). 
- [/lr|--lr|lr|/l_r|--l_r|l_r]: Specify that the following option is the value of $l_R$ (default: $4$). 
- [/h|-h|h|/help|--help|help]: Show this help information. 

#### Usage

- ``python "FS-MUAEKS.py" [/n|-n|n] n [/m|-m|m] m [/q|-q|q] q [/ls|--ls|ls|/l_s|--l_s|l_s] l_S [/lr|--lr|lr|/l_r|--l_r|l_r] l_R``
- ``python "FS-MUAEKS.py" [/h|-h|h|/help|--help|help]``

#### Example

- ``python "FS-MUAEKS.py"``
- ``python "FS-MUAEKS.py" /n 256 /m 4096 /q 256``
- ``python "FS-MUAEKS.py" -n 256 -m 4096 -q 256 --ls 32 --lr 4``
- ``python "FS-MUAEKS.py" n 256 m 4096 q 256 l_s 32 l_r 4``
- ``python "FS-MUAEKS.py" --help``

#### Exit Code

- ``EXIT_SUCCESS`` ($0$): The Python script finished successfully. 
- ``EXIT_FAILURE`` ($1$): The Python script finished not passing all the verifications. 
- ``EOF`` ($-1$): The Python script received unrecognized commandline options. 

#### Note

1) All the commandline options are optional and case-insensitive. 
2) The parameters $n$, $m$, $q$, $l_S$, and $l_R$ should be positive integers and will obey the following priority: values obtained from the command line > values specified by the user within the script > default values set within the script. 
3) The parameters $n$ and $m$ should meet the requirement that $2n | m$. Otherwise, they will be set to their default values respectively. 
