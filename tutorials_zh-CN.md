此文档从 CSDN 迁移至此，由于距离编写已有一段时间，一些观念已经落后，需要整理，而目前此文档还在整理之中，请配合主文档谨慎食用。

# 如何安装 Charm？

## 1. 在 Windows 11 上部署 Ubuntu (WSL)

由于作者没有高性能的 Ubuntu 服务器或个人电脑，且公司或学校提供的 Ubuntu 服务器虽然提供高性能 GPU 等硬件配置但通常不会提供 root 权限，因而作者通过在搭载了 Windows 11 的个人电脑上启动 Ubuntu (WSL) 来进行指引。不使用 WSL 的读者朋友可直接跳过本节。

参考 [https://learn.microsoft.com/zh-cn/windows/wsl/install](https://learn.microsoft.com/zh-cn/windows/wsl/install) 完成 WSL 功能启用重启 Windows 11 后，（以管理员身份）启动命令提示符（按下 Win 键后使用英文输入法输入 cmd 三个字母后单击“命令提示符”或“以管理员身份运行”并授权），在弹出的命令提示符窗口（以下简称“cmd 窗口”）中输入 ``wsl --install Ubuntu`` 并回车。随后，根据提示信息输入用户名（建议全部小写字母）、密码（建议十位以上的综合型密码）和确认密码。这里用户名、密码和确认密码的用途是注册，而不是登录。显示出系统信息和其它信息后，“一台” WSL 就安装完成了，我们也进入到了 WSL 中。
![安装 WSL](https://i-blog.csdnimg.cn/direct/c42970a0dc294e69b30c04092ae7f259.png)
此处，我们使用 Ubuntu 24.04.1 LTS，因为这是我个人认为所有公开的 Linux 操作系统中最好用的一个。为方便起见，用户登录到 WSL 后立即使用 ``sudo passwd root`` 命令指定 root 用户的密码。此处，第一个输入的密码为上面我们注册的用户（例如 universe）的密码，用途是确认用户授权；第二个输入的密码为为新 root 用户指定的新密码，用途为用户注册；第三个输入的密码用于再次确认第二次输入的密码，用途是确认当前用户为新 root 用户指定的新密码，防止用户键入到计算机的密码和用户希望键入到计算机的密码不一致或手误。随后，执行 ``su root`` 命令并输入密码切换到 root，执行 ``cd ~`` 切换到 root 用户的用户目录。
![su root](https://i-blog.csdnimg.cn/direct/61bd0569b6e747549cba4c814d291d65.png)
如有需要，执行 ``apt-get update && apt-get upgrade`` 进行一些更新和升级，在询问是否继续升级时输入 ``Y`` 并回车，也可以直接回车（默认 ``Y``）。有需要的用户也可以更改 apt 源。
![apt-get update && apt-get upgrade](https://i-blog.csdnimg.cn/direct/e5cc50fb5f6b42cb8ba386dd986c0dc3.png)
![apt-get update && apt-get upgrade](https://i-blog.csdnimg.cn/direct/b0b1ac116e34478b96f81854bbb7a946.png)
![apt-get update && apt-get upgrade](https://i-blog.csdnimg.cn/direct/5e49bc3026e441e7bfb535a0a8712f14.png)
附：如果需要退出 WSL 的登录返回到 cmd 中，可以直接在 WSL 的 shell 中执行 ``exit``。退出后，WSL 中的数据会保留。如果哪天不需要这个 WSL 了，可以通过在 cmd 下执行 ``wsl --unregister Ubuntu`` 命令进行对 Ubuntu WSL 的注销。相比于 ``wsl --uninstall Ubuntu``，``wsl --unregister Ubuntu`` 会清空该 WSL 中的所有数据并移除该 WSL，而 若在 ``wsl --uninstall Ubuntu`` 执行后重新执行 ``wsl --install Ubuntu``，会发现之前的数据还在。
![注销 WSL](https://i-blog.csdnimg.cn/direct/1561dc2951594feaa46da58f35528722.png)

## 2. 部署 Python

目前（2024 年 12 月 5 日）Ubuntu 上的情况是：
1) 依照官方 repo 中的一个 Issue（[https://github.com/JHUISI/charm/issues/307](https://github.com/JHUISI/charm/issues/307)），charm 的部署需要 Python 3.10 或以下；
2) 使用 ``apt-get`` 直接安装 Python 默认的版本是 Python 3.12，无法使用 ``apt-get`` 直接安装 Python 3.10；
3) 使用 ``apt-get`` 直接安装 Python-dev 的默认版本是 3.12，无法使用 ``apt-get`` 直接安装 Python3.10-dev；
4) 可以使用 ``wget``、``unzip``、``tar``、``make`` 等命令手动安装 Python 3.10，但搜索了一段时间未找到对应的 Python3.10-dev 安装方式；
5) 无 Python-dev 部署 charm 时，提示需要 Python-dev；
6) 有大佬适配了 Python 3.12 的 charm，位于 [https://github.com/EliusSolis/charm](https://github.com/EliusSolis/charm)，以后估计会被 merge 进官方的 charm。

综合以上情况，我们决定用 Python 3.12 和 [https://github.com/EliusSolis/charm](https://github.com/EliusSolis/charm) 中的 charm 进行部署。
直接执行 ``apt-get install python3 python3-dev python-is-python3``，回车确认，完成安装。
![安装 Python](https://i-blog.csdnimg.cn/direct/dd4cfbe4d768488eb12bd9958cc321dc.png)
运行 ``python`` 进行检查，发现正常。
![python](https://i-blog.csdnimg.cn/direct/5172065c438242b990f2feec9010621c.png)
输入 ``quit()`` 退出 Python，返回到 WSL 的终端。
## 3. GMP
翻阅 charm 官方安装说明 [https://github.com/JHUISI/charm/blob/dev/README.md](https://github.com/JHUISI/charm/blob/dev/README.md) 可知 charm 的部署依赖于 GMP 5.x。不过，GMP 官网（[https://gmplib.org/](https://gmplib.org/)）目前最新版已经是 6.3.0 了。经测试，GMP 6.3.0 也可以，所以，此处使用 GMP 6.3.0。如果有更新版本，请相应替换本节剩余内容中的版本号。
右键单击以下网页截图中箭头指向的位置，复制链接，得到如 ``https://gmplib.org/download/gmp/gmp-6.3.0.tar.xz`` 的链接。在 Ubuntu 中执行 ``wget https://gmplib.org/download/gmp/gmp-6.3.0.tar.xz`` 下载，使用 ``tar -xf gmp-6.3.0.tar.xz`` 或 ``tar -xvf gmp-6.3.0.tar.xz``（显示详细信息）进行解压。也可以下载其它格式，参考 [https://rpubs.com/xliusufe/gmp](https://rpubs.com/xliusufe/gmp) 进行解压。
![在这里插入图片描述](https://i-blog.csdnimg.cn/direct/13c182abdcc24a1e98f14811dfccb013.png)
![GMP](https://i-blog.csdnimg.cn/direct/af878ab5f0d94e6d85258cada8518843.png)
解压后，使用 ``cd gmp-6.3.0`` 进入 gmp-6.3.0 目录。由于需要编译 C/C++ 程序以及 m4，故：
1) 执行 ``apt-get install gcc g++`` 安装 gcc 和 g++；
2) 执行 ``apt-get install m4`` 安装 m4；
3) 执行 ``apt-get install make`` 安装 make。

执行 ``./configure --prefix=/usr --enable-cxx``，其中，第一个参数为更改默认搜索目录为 ``/usr``，第二个参数为启用 C++ 支持。如果提示权限不足（Permisson denied），请先尝试先执行 ``chmod +x ./configure``；如果出现 ``configure: error: could not find a working compiler, see config.log for details``、``configure: error: No usable m4 in $PATH or /usr/5bin (see config.log for reasons).`` 和 ``Command 'make' not found`` 之类的错误提示，那一般就是上面的包没安装好，可以尝试重新执行上面的安装命令，或者卸载重装。
![gcc 和 g++](https://i-blog.csdnimg.cn/direct/b23e53ced7d04590974dd8b09169b63c.png)
![m4](https://i-blog.csdnimg.cn/direct/3742888bc5764daaa2c22b569c21d24f.png)
![make](https://i-blog.csdnimg.cn/direct/d10824b7fffa481b92dfb675f0c5fa90.png)
在命令 ``./configure --enable-cxx`` 执行完成并成功后，依次运行 ``make``、``make check`` 和 ``make install``，其中，可以在 ``make check`` 时检查下是否存在错误或者警告。以下截取了 ``make check`` 过程中的一些绿色的 PASS，看着很开心。这三个命令的执行耗时累计五分钟左右。
![PASS](https://i-blog.csdnimg.cn/direct/ddf9bb7af63a48088d532fe379a755ae.png)
![PASS](https://i-blog.csdnimg.cn/direct/70892841a0f64bcbaab49ebbc92c9116.png)
![make install](https://i-blog.csdnimg.cn/direct/39c1a316b4b7496b8092280877ce5e7c.png)
附：如果在 ``make install`` 之前想修改 ``./configure`` 的参数，可使用 ``make clean`` 进行清理。
## 4. PBC
PBC 库的部署参考于 [https://crypto.stanford.edu/pbc/manual.html](https://crypto.stanford.edu/pbc/manual.html) 和 [https://blog.csdn.net/Frinklin_wang/article/details/123866728](https://blog.csdn.net/Frinklin_wang/article/details/123866728)。
首先，咱们打开 PBC 库的官网 [https://crypto.stanford.edu/pbc/download.html](https://crypto.stanford.edu/pbc/download.html)，找到 Download 下面的表格中的第一个链接，右键复制。
![PBC 官网](https://i-blog.csdnimg.cn/direct/692c13e98db149b6a526ce1d931caefb.png)
使用 ``cd ~`` 返回 root 用户目录，执行 ``wget https://crypto.stanford.edu/pbc/files/pbc-0.5.14.tar.gz`` 下载包。执行 ``tar -zxf pbc-0.5.14.tar.gz`` 或 ``tar -zxvf pbc-0.5.14.tar.gz``（显示详细信息）进行解压。
![PBC](https://i-blog.csdnimg.cn/direct/e4d481a2412b46fea8138f4fd5161eb9.png)
执行 ``apt-get install flex bison`` 并回车确认以安装依赖的包。
![flex 和 bison](https://i-blog.csdnimg.cn/direct/11c1d56723974f08b03b968a10eff1ce.png)
使用 ``cd pbc-0.5.14`` 进入 pbc-0.5.14 目录，执行 ``./configure``。如果提示权限不足（Permission denied），请执行 ``chmod +x ./configure``。在 ``./configure`` 执行成功后，执行 ``make && make install``。
![./configure](https://i-blog.csdnimg.cn/direct/ff0fd431d443458c8f4ae9db866f754c.png)
![make && make install](https://i-blog.csdnimg.cn/direct/524d4c2c4f314cb483788ee690d1df05.png)
![PBC 安装结束](https://i-blog.csdnimg.cn/direct/f3adad32527f4d67ac6193141cbdb864.png)
为测试 pbc 的正确性，使用 ``cd pbc`` 进入 pbc 目录，此时应当位于 ~/pbc-0.5.14/pbc。为方便起见，运行 ``vim test.txt``，按下 ``i`` 键进入编辑模式，将以下内容复制粘贴进去（注意符号都是英文），随后按下键盘左上角的 ESC 键退出编辑模式，输入 ``:x`` 回车退出 vim。顺带声明一下，以下例子来自于本节开头参考的两个链接。
```
g:=rnd(G1);
g;

h:=rnd(G2);
h;

a:=rnd(Zr);
b:=rnd(Zr);
pairing(g^a,g^b);

a:=rnd(Zr);
b:=rnd(Zr);
pairing(g,h)^(a*b);

```
![cd pbc](https://i-blog.csdnimg.cn/direct/21fc0693dbf7499d87ee9ad3db93c5d5.png)
![vim](https://i-blog.csdnimg.cn/direct/98ab61bdb42a40c59b24130e95b3886c.png)
退出 vim 后，可以用 ``cat test.txt`` 查看一下 test.txt 的内容，随后，执行 ``./pbc test.txt``，观察结果。如无意外，一次可能的结果如下：
```
[3329247549575083693704544702300111878032757859527738620078340162954994153076106270578798542088759817755260473344467142914450640537953655213941777251150507, 2915725110970502014368664778822139336115812849055983709686958575727617915386111790813698074081175900851401370141481357430955279929040287141435866041352465]
[3242974909124154401306977997240035331670277658104974758958060345931843939238480034054970695129337969056110321396968088221841267448950244490146519453034952, 4102645750044991539186357580272542381470433262964030405150098834238307121213420471646007670322526602097806928211429728285753402549456577684823579495975023]
[4204905719718175991825854736739721947952505513162738068839979360094249342284680566494097041790152055409462955994730015080480413075607530213740171415894742, 446574590468230745320006695766115100785771408743827211116005662318542764154352629872772863803833470234707075454507234814615174213789732490852008370212665]
[1292042521100579230125067081486676117138594032521849019268142455404207899451482841067146153826541421710150688896771252517403929838056204610903731400443238, 6384245293263856412404284561094069320155145616120241897229969538798442361256734337315733254634859801365184035421441627651994380108240891046561604126951898]
```
![正确性验证](https://i-blog.csdnimg.cn/direct/aba22329ac4c487d8ee68ba2d5b8b601.png)
可以多次执行 ``./pbc test.txt``，但每次结果都是随机的。
![多次执行](https://i-blog.csdnimg.cn/direct/370c95f79a474a45847dfa34ca4bed58.png)
为便于库路径管理，可依次执行以下命令。
```
cd /etc/ld.so.conf.d
echo '/usr/local/lib' > libpbc.conf
cat libpbc.conf
ldconfig
```
![ldconfig](https://i-blog.csdnimg.cn/direct/f98029368d384f188ee65721b7fb62ed.png)
## 5. OpenSSL
使用 ``cd ~`` 返回 root 用户目录。很天真地以为直接 ``apt-get install openssl`` 就可以了。
然而，但是编译错误，说找不到头文件。
参考了下 [https://stackoverflow.com/questions/3016956/how-do-i-install-the-openssl-libraries-on-ubuntu](https://stackoverflow.com/questions/3016956/how-do-i-install-the-openssl-libraries-on-ubuntu)，需要执行 ``apt-get install libssl-dev``，因为要安装的是 lib。还好，也是一个命令的事情。
![OpenSSL](https://i-blog.csdnimg.cn/direct/9218f11771174612b6b954f7c1b97ce6.png)
在 [https://github.com/EliusSolis/charm](https://github.com/EliusSolis/charm) 中找到了链接 [https://github.com/EliusSolis/charm.git](https://github.com/EliusSolis/charm.git)，发现 Ubuntu 自带了 git。于是，直接使用 ``git clone https://github.com/EliusSolis/charm.git``。
![git clone](https://i-blog.csdnimg.cn/direct/49c43fbd70e44c21a7f055aa32d20336.png)
依次执行以下命令完成部署。
```
## chmod +x ./configure
./configure
make install
make test
```
![make install](https://i-blog.csdnimg.cn/direct/7299f56357d9433fb612486344344186.png)
执行到 ``make test`` 时出现了以下意外。
```
/usr/bin/python: No module named pip
error: Command '['/usr/bin/python', '-m', 'pip', '--disable-pip-version-check', 'wheel', '--no-deps', '-w', '/tmp/tmp_srxsing', '--quiet', 'pytest']' returned non-zero exit status 1.
make: *** [Makefile:51: test] Error 1
```
原来是，Python 3.12 的 pip 不会随着 Python 的安装而安装了。事实上，大概从这时候开始，在 Linux 操作系统上，Python 的包逐渐被 Linux 的包管理器接管，例如，安装 pandas 可能需要 ``apt-get install python3-pandas`` 而非 ``python -m pip install pandas`` 或者 ``pip install pandas``（感觉还是以前比较方便且层次分明~~随后开始喜欢继续在 Windows 上运行 Python~~）。于是，执行 ``apt-get install python3-pip`` 并回车确认安装 pip。
继续 ``make test``，通过。
![make test](https://i-blog.csdnimg.cn/direct/73b35f94946d487db93d1cd735b130e5.png)
进入 Python，执行语句 ``from charm.toolbox.pairinggroup import PairingGroup, ZR, G1,G2, GT, pair``，没有报错或警告，即，导入成功。
![import](https://i-blog.csdnimg.cn/direct/f5a0ff7e58a7458d95e687a6ed3bae38.png)
至此，charm 环境部署成功。

## 附录
上述实验在以下两台设备中检验通过：
1) All the experiments conducted in this article are accomplished on 11th Gen Intel(R) Core(TM) i7-11800H CPU 2.30 GHz 8 cores, NVIDIA GeForce RTX 3060 Laptop GPU, 24 GB RAM, 512 GB SSD, and 1024 GB SSD under Windows 11 Pro 24H2 x64. The operating system is on the 512 GB SSD while the codes and the datasets are on the 1024 GB SSD. 
2) All the experiments conducted in this article are accomplished on the AMD Ryzen 7 7735H with Radeon Graphics (8 cores), 16 GB RAM, and 512 GB SSD under Windows 11 Pro 24H2 x64. 

准确一点，可能是：
1) All the experiments conducted in this article are accomplished on **the Ubuntu 24.04.1 LTS platform via the Windows subsystem for Linux (WSL) under** the 11th Gen Intel(R) Core(TM) i7-11800H CPU 2.30 GHz 8 cores, NVIDIA GeForce RTX 3060 Laptop GPU, 24 GB RAM, 512 GB SSD, and 1024 GB SSD under Windows 11 Pro 24H2 x64. The operating system is on the 512 GB SSD while the codes and the datasets are on the 1024 GB SSD. 
2) All the experiments conducted in this article are accomplished on **the Ubuntu 24.04.1 LTS platform via the Windows subsystem for Linux (WSL) under** the AMD Ryzen 7 7735H with Radeon Graphics (8 cores), 16 GB RAM, and 512 GB SSD under Windows 11 Pro 24H2 x64. 

---

# 如何使用 Charm？

一个人熟悉密码学方案但不会代码，一个人熟悉代码但不会密码学方案，于是就有了这篇文章。或许部分学者和程序员会直接去 Github 看代码，但考虑到中文的资料比较少，且对熟悉代码但不会密码学方案的中国新手而言可能有那么一份中文文档会好点，故而写下了这篇博文。本文基于双线性对密码学方案进行，可能不适配其它类型的密码学系统，但代码和思路大同小异，可以参考着迁移。

## 一、环境准备
部署环境可参考 [https://blog.csdn.net/weixin_45726033/article/details/144254189](https://blog.csdn.net/weixin_45726033/article/details/144254189)，此处不再赘述。
一般可以直接用 ``from charm.toolbox.pairinggroup import PairingGroup, G1, G2, GT, ZR, pair, pc_element as Element`` 导入相关依赖。

## 二、密码系统初始化
**描述**：初始化 MNT159 曲线
**Python 代码**：``group = PairingGroup("MNT159")``
**备注**：曲线不存在会报错

**描述**：初始化 SS512 曲线并指定安全参数 $\lambda = 512$
**Python 代码**：``group = PairingGroup("SS512", secparam = 512)``
**备注**：不指定 secparam 时会使用默认的安全参数（事实上 SS512 的默认安全参数就是 512）

**注意**：
1) MNT 系列的曲线和 BN254 曲线是非对称的，SS512 曲线是对称的，且据说 SS512 是目前唯一的素数对称曲线。对于对称方案，如果想做多组实验，建议固定安全参数 $\lambda = 512$ 使用不同曲线进行实验而不是使用相同曲线切换不同的安全参数 $\lambda = 512$ 进行实验。
2) 建议使用面向对象编程；建议将哈希函数以 lambda 或函数的形式存入成员变量 mpk 中，使用 ``callable`` 判断一个变量是否为哈希函数，哈希函数的大小认为是 $\lambda$ bit 或使用 ``(group.secparam + 7) >> 3`` 转为字节（比特填满所有字节后不足 1 字节的视为 1 字节）。
3) 构造 group 时建议环绕一个“try—except”结构（防止用户乱输入）。
4) 由于 ``group = PairingGroup("SS512", secparam = -1)`` 不会报错，在 group 构造完后可以检查 group 的 secparam（防止用户乱输入），如果为非正整数，可以 ``group = PairingGroup(group.groupType())`` 将安全参数重置为默认安全参数或者通过计算阶等其它方式把非正数重置为适当的模 8 正整数或直接截断运行，然后给用户发送一个警告；如果是正整数但不模 8，可以重置，可以截断运行，但鼓励利用 int 适配不模 8 的安全参数，然后给用户发送一个警告。
5) 论文中的 $\mathbb{Z}_r$、$\mathbb{Z}_p$、 $\mathbb{Z}_p^*$、$\mathbb{Z}_q$、 $\mathbb{Z}_q^*$ 在 Python charm 中均为 ZR，里面的元素是一个数而不是一个坐标。

## 三、运算

**描述**：获取群 $\mathbb{G}$ 的阶
**LaTeX**：$p \gets \|\mathbb{G}\|$
**LaTeX 源码**：``$p \gets \|\mathbb{G}\|$``
**Python 代码**：``p = group.order()``

**描述**：随机生成一个 $\mathbb{G}_1$ 的元素
**LaTeX**：generate $g_1 \in \mathbb{G}_1$ randomly
**LaTeX 源码**：``generate $g_1 \in \mathbb{G}_1$ randomly``
**Python 代码**：``g1 = group.random(G1)``
**备注**：G2、GT、ZR 相应进行替换即可

**描述**：随机生成多个 $\mathbb{G}_1$ 的元素（例如 9 个）
**LaTeX**：generate $g_1, g_2, \cdots, g_9 \in \mathbb{G}_1$ randomly
**LaTeX 源码**：``generate $g_1, g_2, \cdots, g_9 \in \mathbb{G}_1$ randomly``
**Python 代码一**：``g1, g2, g3, g4, g5, g6, g7, g8, g9 = group.random(G1, 9)``
**Python 代码二**：``g1, g2, g3, g4, g5, g6, g7, g8, g9 = group.random(G1), group.random(G1), group.random(G1), group.random(G1), group.random(G1), group.random(G1), group.random(G1), group.random(G1), group.random(G1)``
**Python 代码三**（适用于由非常量变量确定元素数量的情况，记得留意列表索引是自然语言中第几个的“几”减去一）：``gVector = [group.random(G1) for _ in range(9)]``
**Python 代码四**（不推荐）：
```
n = 9
for i in range(1, n + 1):
	exec("g{0} = group.random(G1).format(i)")
```
**备注**：G2、ZR 相应进行替换即可（**Python 代码一**不适用于 GT 的 bug 已在 [PR #314](https://github.com/JHUISI/charm/pull/314) 中得到修复）

**描述**：g 为 G1 的一个生成元
**LaTeX**：$g \gets 1_{\mathbb{G}_1}$
**LaTeX 源码**：``$g \gets 1_{\mathbb{G}_1}$``
**Python 代码**：``g = group.init(G1, 1)``
**备注**：g 在 G1、G2、GT 中为生成元而在 ZR 中为单位元

**描述**：加法运算
**LaTeX**：$c = a + b$
**LaTeX 源码**：``$c = a + b$``
**Python 代码**：``c = a + b``
**备注**：加减乘除同（遵从 Python 运算符优先级规则）

**描述**：乘方运算
**LaTeX**：$c = a^b$
**LaTeX 源码**：``$c = a^b$``
**Python 代码**：``c = a ** b``
**备注**：在大多数高级程序语言中 ``^`` 表示异或而不是乘方

**描述**：双线性对元素（非对称曲线下必须一个来自 $\mathbb{G}_1$ 另一个来自 $\mathbb{G}_2$）映射到 $\mathbb{G}_T$ 域
**LaTeX**：$e(p_1, p_2)$
**LaTeX 源码**：``$e(p_1, p_2)$``
**Python 代码**：``pair(p1, p2)``
**备注**：非对称曲线下两元素来自同一个域会报错（另外记得不要在 Python 直接 e）

**描述**：某个密文是一个元组、列表、数组或向量
**LaTeX**：``$\textit{CT} = (a, b, c, d)$``
**LaTeX 源码**：``$\textit{CT} = (a, b, c, d)$``
**Python 代码**：``CT = (a, b, c, d)``
**备注**：``$CT$`` 表示变量 $C$ 乘变量 $T$ 而 ``$\textit{CT}$`` 表示一个名为 CT 的变量（是一个整体）

**描述**：从某个元组、列表、数组或向量中提取变量进行运算
**Python 代码**：``a = CT[0]``
**备注**：可以使用 dict 来进行存储但个人倾向于使用元组因为它是不可变类型、比较接近论文的算法本身且 ``sys`` 中的 ``getsizeof`` 对测量元组类型变量的大小更为准确

**描述**：求逆元
**LaTeX**：$q = p^{-1}$
**LaTeX 源码**：``$q = p^{-1}$``
**Python 代码**：``q = p ** (-1)`` 或 `` q = 1 / p``
**备注**：如果是逆元的平方记得写成 ``p ** (-1) ** 2`` 因为 ``p ** (-2)`` 未定义

**描述**：实现哈希 $H_1$ 将 $\mathbb{Z}_r$ 的元素 $x$ 映射到 $\mathbb{G}_1$ 上（一开始很疑惑一个动作怎么能被作为主公钥的一部分进行存储）
**LaTeX**：$H_1: \mathbb{Z}_r \rightarrow \mathbb{G}_1$
**LaTeX 源码**：``$H_1: \mathbb{Z}_r \rightarrow \mathbb{G}_1$``
**Python 代码**：``H1 = lambda x:group.hash(x, G1)``
**备注**：映射去 $\mathbb{G}_2$ 需要 ``lambda x:group.hash(group.serialize(x), G2)``（先序列化）且该方法对以大多数 ``bytes`` 或能转为 ``bytes`` 类型的元素作为输入的映射都适用

**描述**：实现哈希 $\hat{H}$ 按将元素转为固定长度（单位为 bit）的二进制字符串（以 $\lambda = 512$ 为例）并以 int 的形式存储
**LaTeX**：$\hat{H}: \{0, 1\}^* \rightarrow \{0, 1\}^\lambda$
**LaTeX 源码**：``$\hat{H}: \{0, 1\}^* \rightarrow \{0, 1\}^\lambda$``
**Python 代码**：``HHat = lambda x:int.from_bytes(sha512(group.serialize(x)).digest(), byteorder = "big")``（``from hashlib import sha512``）
**备注**：规则长度通常可以在 hashlib 中找到相应的哈希函数进行实现而不规则长度可通过 SHA512 后复制截断，具体表现为：
- 右对齐式复制截断（b"\0\0\0XXX"）：例如 $\lambda = 1025$ 可以使用 SHA512 的 bytes 形式的最后一个 bit 拼接两个 SHA512 的 bytes 形式 $\rightarrow$ 相当于将 SHA512 值复制形成三份 SHA512 然后利用 ``&`` 进行截断 $\rightarrow$ ``HHat = lambda x:int.from_bytes(sha512(group.serialize(x)).digest() * ((group.secparam - 1) // 512 + 1), byteorder = "big") & ((1 << group.secparam) - 1)``）；
- 左对齐式复制截断（b"XXX\0\0\0"）：例如 ``m, p = b"3", group.random(G1)`` 进行异或操作 $m \oplus \hat{H}(p)$ 可以用以下代码实现；
```
from hashlib import sha512
digest = sha512(group.serialize(p)).digest()
length = max(len(m), len(digest))
r = int.from_bytes(m.ljust(length, b"\0")) ^ int.from_bytes(digest.ljust(length, b"\0")) ## remember b"\0", not b"0"
```
![示意图](https://i-blog.csdnimg.cn/direct/0538b285096e4cce8871b727441cbcf8.png)
- 少数方案存在哈希的输出为 $\lambda$ 倍数的情况，在测量 mpk 长度时可能需要独立编写对这些哈希函数的长度计算过程。

## 四、类型转换

隐式转换（Element 内）

- 两个同类元素加减乘除得到的元素依旧是该类型
- G1、G2、GT 乘以他们所属群的生成元等于它们自己
- 不同群之间的元素无法做加法、减法或除法运算
- ZR 元素乘以 G1 元素得到 G1 元素
- ZR 元素乘以 G2 元素得到 G2 元素
- ZR 元素乘以 GT 元素得到 GT 元素
- pair(G1, G2) 或 pair(G2, G1) 得到 GT 元素
- 做指数运算时只有 ZR 元素可以为指数且运算说得的元素类型与底数保持一致

显式转换

- 从 ``int`` 到 ``bytes``：``x.to_bytes(digitCount, byteorder = "big")``（``digitCount`` 是字节长度）
- 从 ``bytes`` 到 ``int``：``int.from_bytes(x, byteorder = "big")``
- 从 ``Element`` 到 ``bytes``：``pairingGroup.serialize(x)``（``pairingGroup`` 是 ``PairingGroup`` 的一个实例）
- 从 ``bytes`` 到 ``Element``：``pairingGroup.hash(x, elementType)``（``pairingGroup`` 是 ``PairingGroup`` 的一个实例并且 ``elementType`` 只能是 ZR 或者 G1）
- 异或：切换为 ``int`` 后执行
- 非矩阵连接类型的连接：切换为 ``bytes`` 后执行

在显示转换中，仅有以下两种情况是等效的。

- 仅当固定了具体的字节长度且运算没有溢出该长度时，int 或 bytes 其中的一个类型在转换为另一个类型后执行有限的运算后再转换回原有类型与保持原有类型执行同样的有限运算是等效的。
- 仅当没有任何额外操作时，在同一个 group 中将元素序列化再反序列化能够得到与原来一致的元素。

## 五、异或

异或，通常由 ``int`` 代理实现，因而要想方设法将各种类型转为 ``int`` 进行异或。密码学论文在写作的时候几乎不会考虑类型转换的问题，他们认为 Element、int、string、binary String 等都是一样的，因为可以互转。在实现中，异或过程中考虑类型转换问题往往是编写方案代码中与连接问题并称最为耗时的两环。无论是异或还是连接，一般从 ``Element`` 转为代理类型（如异或的 ``int``）后就不会转回去，因为异或和连接都不是 ``Element`` 支持的运算。同时，纵使 ``Element`` 支持这两个运算，转为代理类型处理完后再转回去得到的 ``Element`` 与两个 ``Element`` 之间真正意义上的运算并不是等效的，即，将两个 ``Element`` 序列化后转 ``int`` 异或再还原得到的 ``bytes`` 将难以重新映射到正确的 ``Element`` 上（即使序列号时去除了类型前缀），甚至报错，可以简单理解为 ``c = a + b`` 后无法通过 ``a = c - b`` 还原 ``a``。因此，也不需要写一个泛型来直接兼容 ``Element``、``int`` 和 ``bytes``（甚至比特流）使得运算过程中不需要考虑类型转换。要保证方案闭环（学术上的说法为合法性、正确性等检验通过），只需确保对应位置使用了相同的哈希、异或运算、连接运算和长度即可。
![不等效运算](https://i-blog.csdnimg.cn/direct/d4fa754b32124ee2a125ae9a70533395.png)

与其它类型相比，``int`` 的优势在于高效，劣势则为无法保留长度信息（``bytes`` 的长度信息也只能精确到字节）。一般而言，异或前后的数据类型通常由 ``int`` 存储，并利用 $\lambda$ 控制长度。在遇到极端情况（例如方案要求严格控制 bit 数量）时，``int`` 和 ``bytes`` 通常都无法实现严格控制，通常需要寻求比特流的帮助（或自己实现一个基于序列数据类型的类），并使用一个额外的变量保存比特流的长度，但这会让程序运行效率极度下降。

**描述**：两个 ``int`` 异或（**本质代码**）
**LaTeX**：$c = a \oplus b$, where $a$ and $b$ are two integers
**LaTeX 源码**：``$c = a \oplus b$, where $a$ and $b$ are two integers``
**Python 代码**：``c = a ^ b``
**备注**：多个就直接用 ``^`` 连接起来

**描述**：两个或多个（不等长的）``bytes`` 异或（**最常用**）
**LaTeX**：$c = a \oplus b$, where $a$ and $b$ are two binary strings
**LaTeX 源码**：``$c = a \oplus b$, where $a$ and $b$ are two binary strings``
**Python 代码一**（最推荐，转 ``int`` 处理）：
```
int.from_bytes(a, byteorder = "big") ^ int.from_bytes(b, byteorder = "big")
```
**备注**：如需转回 ``bytes`` 需要自行斟酌长度或使用动态适配（动态适配可能会导致测量和明文还原问题）
**Python 代码二**（截断式，按最小长度保留左数若干个字节，利用 ``bytearray``）：
```
def xor(*bElements:bytes) -> bytes:
	if bElements and all([isinstance(bEle, bytes) for bEle in bElements]):
		minLength = min([len(bEle) for bEle in bElements])
		bResult = bytearray(minLength)
		for i in range(minLength):
			bResult[i] = bElements[0][i]
			for bElement in bElements[1:]:
				bResult[i] ^= bElement[i]
		return bytes(bResult)
	else:
		return b""
```
**Python 代码三**（向左对齐最大长度，在右边补 ``b"\0"`` 对齐最大长度，利用 ``bytearray``）：
```
def xor(*vec:bytes) -> bytes:
	if vec and all([isinstance(v, bytes) for v in vec]):
		maxLength = max([len(v) for v in vec])
		bElements = [v.ljust(maxLength, b"\0") for v in vec]
		bResult = bytearray(maxLength)
		for i in range(maxLength):
			bResult[i] = bElements[0][i]
			for bElement in bElements[1:]:
				bResult[i] ^= bElement[i]
		return bytes(bResult)
	else:
		return b""
```
**Python 代码四**（向左对齐最大长度，在右边补 ``b"\0"`` 对齐最大长度，利用 ``int``）：
```
def xor(*vec:bytes) -> bytes:
	if vec and all([isinstance(v, bytes) for v in vec]):
		maxLength = max([len(v) for v in vec])
		bElements = [int.from_bytes(v.ljust(maxLength, b"\0"), byteorder = "big") for v in vec]
		iResult = bElements[0]
		for bEle in bElements[1:]:
			iResult ^= bEle
		return iResult.to_bytes(maxLength, byteorder = "big")
	else:
		return b""
```

**描述**：将元素的哈希映射后的二进制字符串形式、二进制字符串形式的消息进行异或运算
**LaTeX**：$c = \hat{H}(a) \oplus \hat{H}(b) \oplus m$
**LaTeX 源码**：``$c = \hat{H}(a) \oplus \hat{H}(b) \oplus m$``
**Python 代码**：请参考上一节最后一个示例的第二个备注
**备注**：哈希过去后回不来

**描述**：将两个元素的序列化值异或并将使得出结果为 $\mathbb{Z}_r$ 的元素（可能没有实际意义）
**Python 代码**：``c = group.init(ZR, int.from_bytes(group.serialize(a), byteorder = "big") ^ int.from_bytes(group.serialize(b), byteorder = "big"))``
**备注**：利用序列化转 bytes $\rightarrow$ 利用大端序将 bytes 转 int $\rightarrow$ 利用 int 的异或进行运算 $\rightarrow$ 转回相应的域元素

**描述**：将两个属于 $\mathbb{G}_1$、$\mathbb{G}_2$ 或 $\mathbb{G}_T$ 的元素的坐标进行异或（可能没有实际意义）
**Python 代码**：
```
>>> from charm.toolbox.pairinggroup import PairingGroup, GT
>>> group = PairingGroup('SS512')
>>> val1, val2 = group.random(GT), group.random(GT)
>>> from ast import literal_eval
>>> a = literal_eval(str(val1))
>>> b = literal_eval(str(val2))
>>> c = [a[0] ^ b[0], a[1] ^ b[1]]
>>> print(c)
[3145433883534124401324439347768110835398028350614674524059560171517004111791478876754194360139431612102643110791417360856381148035943988953848930714123517, 2857624943437781882203759044144741085626969501834396600205581471476136669068330544718339201855808681161256560385959005578605996128415003896164989686702349]
>>>
```
**备注**：变不回 ``Element`` 类型

**描述**：将两个属于 $\mathbb{Z}_r$ 的元素的坐标进行异或（可能没有实际意义）
**Python 代码**：
```
>>> from charm.toolbox.pairinggroup import PairingGroup, ZR
>>> group = PairingGroup('SS512')
>>> val1, val2 = group.random(ZR, 2)
>>> from ast import literal_eval
>>> c = literal_eval(str(val1)) ^ literal_eval(str(val2))
>>> val = group.init(ZR, c)
>>> print(val)
99148580641080189363891475714904521534661686807
```

**描述**：将两个元素异或（常用于加解密完成了所有 ``Element`` 和其它运算后用于将明文异或为密文或将密文异或为明文的最后一步）
**方法**：将两个元素转为 ``int`` 后异或即可（转 ``int`` 不可也无需返回 ``Element`` 继续进行运算）

## 六、连接（非矩阵连接）

连接，通常由 ``bytes`` 代理实现。``bytes`` 连接用 ``+``，其它类型设法转为 ``bytes`` 进行连接即可，但存储时通常使用 ``int``。部分方案认为连接后得到的整体可以依照长度通过切割还原为原有的多个变量。关于正确性，请参阅异或部分说明。

## 七、测量
1) 测量某段代码的运行时间（用 ``time()`` 可能会因精度问题出现耗时为负值）
```
from time import perf_counter

startTime = perf_counter()
## Your codes
endTime = perf_counter()
timeDelta = endTime - startTime
```

2) （工程用途上）测量某个变量的实际存储空间（单位：字节）
```
from sys import getsizeof

s = getsizeof(group.random(ZR))
```

3) （学术用途上）测量某个变量的长度（单位：字节）：利用序列化转为 ``bytes`` 对象之后测长度
```
def getLengthOf(group:object, obj:Element|tuple|list|set|bytes|int) -> int:
	if isinstance(obj, Element):
		return len(group.serialize(obj))
	elif isinstance(obj, (tuple, list, set)):
		sizes = tuple(getLengthOf(o) for o in obj)
		return -1 if -1 in sizes else sum(sizes)
	elif isinstance(obj, bytes):
		return len(obj)
	elif isinstance(obj, int) or callable(obj):
		return group.secparam >> 3
	else:
		return -1
```

4) （工程用途上）测运行时程序占用的运行内存（单位：字节）
```
import os
try:
	from psutil import Process
except:
	print("Cannot compute the memory via ``psutil.Process``. ")
	print("Please try to install the ``psutil`` library via ``python -m pip install psutil`` or ``apt-get install python3-psutil``. ")
	print("Please press the enter key to exit. ")
	input()
	exit(-1)

process = Process(os.getpid())
memory = process.memory_info().rss
```

## 八、一些调试经历

1) 导入的 G1、G2、GT、ZR 的类型其实是 ``int``：
![在这里插入图片描述](https://i-blog.csdnimg.cn/direct/0b39fb6c3c56475ab8eabb98965d469e.png)
2) 可以在关键处用 ``print`` 和 ``input`` 下断点调试观察比较：
![断点调试](https://i-blog.csdnimg.cn/direct/16da16029df248a5ab33fb1dfe23def5.png)
3) 牢记 1 字节等于 8 比特，遇到 $\lambda$（单位为位）为不常规数值时建议进行过滤或警告，例如过滤非正整数，在容错范围内将不模 8 的 $\lambda$ 进行特殊处理。
4) RSA 中变量 $p$、$q$ 的长度不是 RSA 的安全参数，双线性对映射中 ``PairingGroup`` 的 ``secparam`` 可能没有实际意义，长度貌似是固定的，在实际实现时可能达不到 $\lambda$ 的安全级别。
5) 最后就是，从学术的角度讲可能代码并不太重要，审稿人注重理论上的安全性分析也基本不会去跑代码，即使跑代码了也不会去纠结这些边界情况；但，从工程的角度来讲，有能力把代码写得更强壮就应当让代码尽可能强壮，在程序基本完成后这些边界情况往往是最容易被忽视却又是最棘手、最致命、最需要得到关注的地方；该检查合法性时就检查合法性，能容错时就尽可能容错，该提示用户时就提示用户，该修正时就修正，该截断运行时就截断运行。安全多方计算方面提出新密码学方案的论文在谷歌学术上能查到很多，库的开发文档也较为完备，但基于 C/C++ PBC、Python charm 和 JPBC 的引导性资料却不多，能把代码下载下来直接运行成功的代码也不多。

如遗漏重要的运算，可私信更新。圣诞快乐，新年快乐！

---

# 如何安装 Crypto 库？

测试[一些密码学方案](https://github.com/xuehuan-yang/PSME/blob/main/src/common/image.py)需要用到 Crypto 库，网上教程大多针对 Windows 和 Python 3.10 或以下的环境，所以写下了这篇博文。

### 部署与使用

首先执行 ``su`` 输入密码进入超级用户，部署完 Python 3.12 环境后，执行以下命令进行安装（如果之前有安装过旧版可能需要先进行清除具体操作请参考其它教程）。
```
apt-get install python3-pycryptodome
```
![安装](https://i-blog.csdnimg.cn/direct/afd208133d124dbd8594f99332672647.png)


执行以下命令进行测试，看到 ok 就行啦！
```
python3 -m Cryptodome.SelfTest
```
![测试](https://i-blog.csdnimg.cn/direct/2c1a7c47e7b3403fa8daeb7721cdcc8b.png)

使用时，需要将 Crypto 替换为 Cryptodome，例如，需要将 ``from Crypto.Cipher import AES`` 修改为 ``from Cryptodome.Cipher import AES``。

### 反思

发现在 Ubuntu 24.04.1 LTS | Python 3.12 环境下很多 Python 库的安装命令都变成了 ``apt-get install python3-XXX`` 的形式，例如安装 ``pandas`` 从 ``python3 -m pip install pandas`` 变成了 ``apt-get install python3-pandas``。

一方面，Python 的库安装需要超级用户（Linux）或管理员（Windows）权限，缺乏足够的权限很可能导致 defaulting to XXX 的局面，随后就是包装得到处都是；另一方面，在 Linux 操作系统上，Python 官方提示使用 root 进行 Python 的库安装会产生较高的风险。所以，能够理解，在 Linux 操作系统上，Python 3.12 将 Python 的库管理托管给 apt 进行，这样既解决了权限问题，又降低了以 root 身份执行 Python 库安装的风险。

### 参考文献

- [https://blog.csdn.net/weixin_55024601/article/details/136062283](https://blog.csdn.net/weixin_55024601/article/details/136062283)
- [https://pycryptodome.readthedocs.io/en/latest/src/installation.html](https://pycryptodome.readthedocs.io/en/latest/src/installation.html)
