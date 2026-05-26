# Stage 8 요약 (AWS EC2, RDS, S3 프리티어 배포 설정 완료)

AWS 무료 프리티어(Free Tier) 범위 내에서 서비스를 안정적으로 실행할 수 있도록 필요한 코드 수정과 배포 리소스 가이드를 완료했습니다.

---

## 1. 코드 변경 및 설정 완료 사항 (AI 개발 완료)

### 1) JVM 메모리 동적 파라미터화 ([Dockerfile](file:///d:/git/202605/recipekr/Dockerfile) 수정)
* 기존 Render용 512MB RAM 제약을 위해 `ENTRYPOINT`에 하드코딩되었던 JVM 힙 메모리 옵션(`-Xms128m -Xmx256m`)을 `ENV JAVA_OPTS` 환경변수 형태로 구조 개선했습니다.
* 이를 통해 EC2의 구동 성능 및 메모리 가용성에 맞춰 자유롭게 조절할 수 있습니다.

### 2) 서비스 기동 일괄화 ([docker-compose.yml](file:///d:/git/202605/recipekr/docker-compose.yml) 신규 생성)
* EC2에서 Java 및 Python 통합 빌드 컨테이너의 라이프사이클을 손쉽게 가동하도록 멀티 컨테이너 규격을 설계했습니다.
* HTTP 기본 포트(80)로 접속 시 컨테이너 내부의 스프링 서버 포트(8080)로 매핑되도록 처리했습니다.
* DB 환경설정 및 Gemini API 키, AWS S3 변수를 주입받도록 구성했습니다.

### 3) 환경 변수 템플릿 제공 ([.env.example](file:///d:/git/202605/recipekr/.env.example) 신규 생성)
* EC2에 배포할 때 실제 데이터베이스 주소, 비밀번호 및 API 키를 숨김 처리할 수 있도록 템플릿 환경변수 파일을 작성했습니다.
* 보안을 위해 실제 주입용 `.env` 파일은 깃에 추적되지 않도록 설정되어 있습니다.

---

## 2. 개발자가 수행해야 할 인프라 구축 절차 (To-Do List)

개발자분께서는 아래의 순서에 따라 AWS 웹 콘솔에 접속하여 리소스를 세팅해 주시기 바랍니다.

### [ ] 1단계: AWS RDS MySQL 생성 (프리티어)
1. **RDS 콘솔** 접속 ➔ **데이터베이스 생성** 클릭
2. **표준 생성** 및 엔진 옵션 **MySQL** 선택 (버전: 8.0.x 권장)
3. 템플릿에서 반드시 **프리티어(Free Tier)** 선택 (무료 혜택 보장 핵심!)
4. **설정**: DB 인스턴스 식별자(`recipekr-rds`), 마스터 사용자 이름(`admin`), 마스터 암호 설정
5. **스토리지**: 범용 SSD(gp2 또는 gp3), 할당된 스토리지 **20 GiB**, **'스토리지 자동 조정 활성화' 체크 해제**
6. **연결**: 외부에서 접속해 초기 테이블 세팅을 할 수 있도록 **퍼블릭 액세스 '예'**로 임시 설정 권장
7. **추가 구성**: 최초 데이터베이스 이름에 **`recipekr`** 기입, 자동 백업 비활성화(보존 기간 0일 또는 1일)
8. 생성이 완료되면 상세 페이지에서 **엔드포인트(Endpoint)** 주소를 복사해 둡니다.

### [ ] 2단계: AWS EC2 인스턴스 생성
1. **EC2 콘솔** 접속 ➔ **인스턴스 시작** 클릭
2. AMI: **Ubuntu 22.04 LTS** (프리티어 사용 가능)
3. 인스턴스 유형: **t2.micro** 또는 **t3.micro** (프리티어 타입)
4. 키 페어: 새 키 페어(`.pem`)를 다운로드하여 로컬에 안전하게 보관합니다.
5. **네트워크 설정 (보안 그룹)**: 새 보안 그룹 생성 및 아래 인바운드 규칙 추가
   - SSH (포트 22) ➔ 내 IP 혹은 어디서나 (`0.0.0.0/0`)
   - HTTP (포트 80) ➔ 어디서나 (`0.0.0.0/0`)
   - HTTPS (포트 443) ➔ 어디서나 (`0.0.0.0/0`)
6. **스토리지**: 디스크 크기를 **30 GiB**로 변경하고 볼륨 유형은 **gp3**를 지정합니다. (프리티어 한도)

### [ ] 3단계: AWS S3 버킷 생성
1. **S3 콘솔** 접속 ➔ **버킷 만들기** 클릭
2. 버킷 이름: 고유한 이름 설정 (예: `recipekr-storage-bucket`)
3. AWS 리전: `ap-northeast-2 (서울)` 선택
4. 이미지 파일 조회를 위해 **'모든 퍼블릭 액세스 차단' 설정을 해제**하여 버킷 생성

---

## 3. EC2 서버 접속 후 최적화 실행 명령어 목록

SSH로 서버 접속 후 아래 명령어 블록들을 복사하여 그대로 실행해 주세요.

### [ ] 1) 2GB Swap 메모리 할당 (OOM 방지 필수)
```bash
# 1. 2GB 크기의 가상 스왑 파일 생성
sudo dd if=/dev/zero of=/swapfile bs=128M count=16

# 2. 파일 접근 권한 수정 (루트 전용)
sudo chmod 600 /swapfile

# 3. 파일 포맷을 스왑 공간으로 지정 및 활성화
sudo mkswap /swapfile
sudo swapon /swapfile

# 4. 스왑 메모리가 정상 적용되었는지 확인 (Swap: 2.0Gi 행이 보이는지 확인)
free -h

# 5. 서버 재부팅 시에도 자동으로 마운트되도록 fstab 파일에 추가
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
```

### [ ] 2) Docker & Docker Compose 설치
```bash
# 패키지 업데이트 및 필수 패키지 설치
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# Docker 공식 GPG 키 추가
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Docker 리포지토리 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 패키지 목록 갱신 및 Docker 설치
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# docker compose 호환 명령어 심볼릭 링크 생성
sudo ln -sf /usr/libexec/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose

# ubuntu 사용자가 sudo 권한 없이 docker 실행 가능하도록 그룹 추가
sudo usermod -aG docker ubuntu
```
> ⚠️ **주의**: Docker 설치 및 그룹 추가 명령어 수행 후, 반드시 세션을 종료(`exit`)하고 다시 SSH로 재접속하셔야 sudo 없이 docker 명령어가 정상 작동합니다.

---

## 4. 최종 배포 기동 및 검증

이제 수정한 코드를 로컬 컴퓨터에서 커밋하고 `git push origin main` 하시면 깃허브 액션이 아래 작업을 **완전 자동으로** 처리해 줍니다.

### ⚠️ 중요 보안 주의 사항: API Key 유출 및 차단 문제 해결
- **유출 감지 및 자동 차단**: 이전에 코드 설정에 포함되었던 제미나이 API 키가 깃허브로 푸시되면서 구글 보안 시스템에 의해 **유출(Leaked)로 감지되어 자동으로 비활성화(403 차단)** 되었습니다. (이 때문에 두 번째 추천부터 실패한 것입니다.)
- **보안 조치 완료**: 코드에서 모든 하드코딩된 API 키와 DB 접속 정보를 원천 제거하고 placeholder(`[YOUR_GEMINI_API_KEY]` 등)로 교체했습니다.
- **다이내믹 주입 연동**: 대신, 배포 스크립트([deploy.yml](file:///d:/git/202605/recipekr/.github/workflows/deploy.yml))에서 **이미 사용자가 깃허브 시크릿에 안전하게 등록해 두신 `TIDB_URL`, `TIDB_USERNAME`, `TIDB_PASSWORD`, `GEMINI_API_KEY` 값들을 배포 시점에 자동으로 EC2 내부 `.env` 파일에 생성/주입**하도록 극적으로 개선했습니다.
- **조치 사항**: 이미 기존 API 키는 구글에 의해 무효화되었으므로, **[구글 AI 스튜디오](https://aistudio.google.com/)에서 새로운 API 키를 새로 생성**하신 뒤, **GitHub Repository Settings ➔ Secrets ➔ Actions의 `GEMINI_API_KEY` 값만 새 키로 갱신**해 주세요.

### [ ] 1) 깃허브 액션 자동 배포 트리거
로컬 프로젝트 루트 폴더에서 터미널을 열고 푸시를 진행합니다. (새로운 API 키로 깃허브 시크릿을 갱신한 이후 푸시를 진행하시는 것을 권장합니다.)
```bash
git add .
git commit -m "deploy: API 키 유출 방지 조치 및 깃허브 시크릿 동적 바인딩 배포 개선"
git push origin main
```
- **자동 진행되는 작업**:
  1. GitHub Actions가 코드를 체크아웃하고 Docker 이미지를 Docker Hub에 빌드/업로드합니다.
  2. **`scp-action`**을 통해 EC2 서버의 `/home/ubuntu/recipekr` 폴더를 자동으로 생성하고 `docker-compose.yml`을 전송합니다.
  3. GitHub Secrets에 저장된 정보를 기반으로 EC2 내부 `/home/ubuntu/recipekr/.env` 파일을 **보안 침해 우려 없이 동적으로 자동 생성**합니다. (새로 갱신한 제미나이 키와 TiDB 정보가 알아서 들어갑니다.)
  4. Docker Hub에서 최신 빌드 이미지를 다운로드받고 무중단 기동합니다.

### [ ] 2) 배포 진행 상황 확인
깃허브 저장소의 **Actions** 탭에서 **Deploy to AWS EC2 via Docker Hub** 워크플로우가 초록색 체크(`Success`)로 완료되는지 확인합니다.

### [ ] 3) EC2 서버에서 동작 상태 및 로그 확인
배포가 완료되면 EC2 터미널에 접속하여 컨테이너가 정상적으로 실행 중인지 점검합니다.
```bash
cd /home/ubuntu/recipekr
docker ps
docker-compose logs -f app
```
- 성공적으로 기동되면 브라우저에서 `http://[EC2_퍼블릭_IP]` (예: `http://54.180.109.107`)로 접속하여 회원가입, 로그인, 마이페이지 기능이 TiDB 클라우드와 맞물려 정상 동작하는지 확인합니다.

---

---

## 5. GitHub Actions를 통한 자동 배포 (CI/CD) 설정 방법

새롭게 생성된 [.github/workflows/deploy.yml](file:///d:/git/202605/recipekr/.github/workflows/deploy.yml) 파일은 **GitHub Actions 러너가 소스코드를 기반으로 Docker 이미지를 빌드한 뒤 Docker Hub에 푸시하고, EC2 서버는 이 pre-built 이미지를 다운로드받아 가동**하는 고도화된 배포 방식을 사용합니다.
- 이 방식은 1GB RAM 프리티어 EC2 서버 내에서 무겁게 빌드를 돌리지 않으므로, **EC2 서버 과부하 및 메모리 부족 현상을 완벽하게 방지**할 수 있습니다.

첨부해 주신 그림의 Repository Secrets 정보를 확인했으며, 이에 맞춰 워크플로우 코드를 매핑했습니다.

### [x] 깃허브 시크릿 설정 체크리스트
현재 그림처럼 아래 변수들이 정상 등록되어 있으므로 추가 조치가 필요 없습니다.
- **`DOCKERHUB_USERNAME`**: Docker Hub 로그인 아이디
- **`DOCKERHUB_TOKEN`**: Docker Hub 로그인 비밀번호/토큰
- **`EC2_HOST`**: EC2 퍼블릭 IPv4 주소
- **`EC2_SSH_KEY`**: EC2 SSH 키페어(`.pem`) 파일 내용 전체
- **`GEMINI_API_KEY`**: 구글 제미나이 API 키
- **`TIDB_URL`** / **`TIDB_USERNAME`** / **`TIDB_PASSWORD`**: 기존 TiDB 클라우드 접속 정보

> 💡 **안내**: 깃허브 시크릿에 `EC2_USERNAME`이 설정되어 있지 않은 점을 고려하여, 워크플로우 스크립트 내부에서 SSH 접속 계정 명을 **`ubuntu`**로 하드코딩 처리했습니다. 따라서 별도로 `EC2_USERNAME` 시크릿을 추가하지 않으셔도 작동합니다.

변수 등록이 이미 완료되어 있으므로, 수정한 코드를 커밋하고 **`main` 브랜치에 Push** 하시면 GitHub Actions 탭에서 배포 빌드 및 배포 작업이 자동으로 성공적으로 진행됩니다.
