# Stage 9: AWS RDS(MySQL) 생성 및 연결 가이드

본 문서는 AWS 콘솔에 로그인한 후 RDS(Relational Database Service) MySQL 인스턴스를 생성하고, 로컬 VSCode에서 원격으로 연결하기까지의 전체 과정을 상세히 설명합니다.

---

## 1. AWS 로그인 및 RDS 대시보드 이동

1. **AWS 관리 콘솔(AWS Management Console)**에 로그인합니다. (루트 사용자 또는 IAM 사용자)
2. 화면 상단의 검색창에 **"RDS"**를 입력하고, 나타나는 서비스 목록에서 **RDS**를 클릭합니다.
3. 좌측 메뉴 또는 메인 대시보드에서 **[데이터베이스 생성(Create database)]** 버튼을 클릭합니다.

---

## 2. 데이터베이스 생성 기본 설정

데이터베이스 생성 방식과 엔진 옵션을 선택합니다.

*   **데이터베이스 생성 방식 선택**: **표준 생성(Standard create)** 선택
*   **엔진 옵션(Engine options)**: **MySQL** 선택
*   **엔진 버전(Engine version)**: 기본으로 선택된 버전 유지 (예: MySQL 8.0.x)
*   **템플릿(Templates)**: **프리티어(Free tier)** 선택
    > [!IMPORTANT]
    > 과금을 방지하려면 반드시 '프리티어'를 선택해야 합니다. 프리티어 옵션이 보이지 않을 경우, 현재 리전이나 계정 상태를 확인하세요.

---

## 3. 설정 (Settings)

데이터베이스 식별자와 로그인 정보를 설정합니다.

*   **DB 인스턴스 식별자(DB instance identifier)**: RDS 인스턴스의 이름입니다. (예: `recipekr-db`)
*   **마스터 사용자 이름(Master username)**: 데이터베이스 최고 관리자 계정 이름입니다. (기본값인 `admin`을 사용하거나 원하는 이름 입력)
*   **마스터 암호(Master password)**: 접속에 사용할 비밀번호를 입력하고 한 번 더 확인 입력합니다.
    > [!WARNING]
    > 이 사용자 이름과 암호는 나중에 VSCode에서 연결하거나 스프링부트(Spring Boot) 설정 파일에 넣어야 하므로 반드시 기억(또는 메모)해 두어야 합니다.

---

## 4. 인스턴스 및 스토리지 구성

프리티어 한도 내에서 자원을 할당합니다.

*   **인스턴스 구성(Instance configuration)**: 프리티어 기본값인 `db.t3.micro` 또는 `db.t4g.micro`가 선택되어 있는지 확인합니다.
*   **스토리지(Storage)**:
    *   스토리지 유형: 범용 SSD (gp2)
    *   할당된 스토리지: 20 (기본값, 최대 20GB까지 프리티어 적용)
    *   **스토리지 자동 조정 활성화(Enable storage autoscaling)**: **체크 해제**
        > [!TIP]
        > 자동 조정을 켜두면 데이터가 늘어날 때 자동으로 용량이 증가하여 추가 과금이 발생할 수 있으므로, 학습/토이 프로젝트에서는 꺼두는 것이 좋습니다.

---

## 5. 연결 (Connectivity) - 네트워크 및 보안 설정

가장 중요한 네트워크 접근 설정 단계입니다.

*   **Virtual Private Cloud (VPC)**: 기본값(Default VPC) 유지
*   **퍼블릭 액세스(Public access)**: **예(Yes)** 선택
    > [!CAUTION]
    > 로컬 PC(VSCode 등)에서 인터넷을 통해 AWS에 있는 DB에 직접 붙기 위해서는 반드시 퍼블릭 액세스를 "예"로 설정해야 합니다. (이후 보안 그룹에서 특정 IP만 허용하도록 제한합니다.)
*   **VPC 보안 그룹(VPC security group)**: **새로 생성(Create new)** 선택 (또는 기존에 설정해둔 것이 있다면 선택)
*   **새 VPC 보안 그룹 이름**: `recipekr-db-sg` 등 식별하기 쉬운 이름 입력
*   **데이터베이스 포트(Database port)**: 3306 (기본값)

---

## 6. 추가 구성 (Additional configuration)

초기 데이터베이스와 파라미터 그룹 등을 설정합니다. (화면 하단의 '추가 구성' 토글을 열어야 보입니다.)

*   **초기 데이터베이스 이름(Initial database name)**: `recipekr` (원하는 DB 이름 입력. 이 이름을 입력하면 RDS 생성 시 해당 이름의 데이터베이스가 자동 생성됩니다.)
*   **DB 파라미터 그룹(DB parameter group)**: 기본값 유지
    * *한글 인코딩 문제가 발생할 경우 나중에 커스텀 파라미터 그룹을 생성해 `utf8mb4` 설정을 적용해야 하지만, 최신 MySQL 8.0 이상에서는 기본적으로 `utf8mb4`가 지원됩니다.*
*   **자동 백업(Automated backups)**: 비활성화 (학습용일 경우 과금 방지 및 용량 절약을 위해 백업을 끄는 것이 유리할 수 있습니다.)

설정을 모두 확인했으면 맨 아래의 **[데이터베이스 생성(Create database)]** 버튼을 클릭합니다. 생성 완료까지 약 3~5분 정도 소요됩니다.

---

## 7. 보안 그룹 인바운드 규칙 편집 (IP 접근 허용)

RDS가 생성되는 동안, 설정한 보안 그룹의 포트를 열어주어야 합니다.

1. RDS 대시보드 인스턴스 목록에서 방금 생성 중인 DB 이름을 클릭합니다.
2. [연결 및 보안(Connectivity & security)] 탭에서 **'VPC 보안 그룹'** 아래에 있는 링크(예: `recipekr-db-sg`)를 클릭합니다. (EC2 보안 그룹 콘솔로 이동됩니다.)
3. 하단의 **[인바운드 규칙(Inbound rules)]** 탭을 클릭하고 **[인바운드 규칙 편집(Edit inbound rules)]** 버튼을 누릅니다.
4. 규칙 추가:
    *   **유형**: MySQL/Aurora
    *   **포트 범위**: 3306
    *   **소스(Source)**: `내 IP(My IP)`를 선택하여 현재 PC에서만 접근하게 하거나, 개발 편의성을 위해 `위치 무관(Anywhere-IPv4)` (`0.0.0.0/0`)으로 설정합니다.
        > [!WARNING]
        > 실무 환경에서는 `0.0.0.0/0` 설정은 심각한 보안 취약점이 됩니다. 반드시 `내 IP`나 지정된 서버 IP만 허용하는 것이 원칙입니다.
5. **[규칙 저장(Save rules)]**을 클릭합니다.

---

## 8. 엔드포인트 확인 및 VSCode(Database Client) 연결

RDS 인스턴스 상태가 "사용 가능(Available)"으로 변경되면 연결을 시도할 수 있습니다.

1. RDS 상세 페이지의 [연결 및 보안] 탭에서 **엔드포인트(Endpoint)** 문자열을 복사합니다. (예: `recipekr-db.xxxxxxxxx.ap-northeast-2.rds.amazonaws.com`)
2. VSCode를 열고 좌측의 **Database Client** 아이콘을 클릭합니다.
3. `+` 버튼을 눌러 새 연결 생성 창을 열고, 아래와 같이 정보를 입력합니다.
    *   **Connection Type / Database**: MySQL
    *   **Host**: 복사한 AWS RDS 엔드포인트
    *   **Port**: 3306
    *   **Username**: 앞서 설정한 마스터 사용자 이름 (예: `admin`)
    *   **Password**: 앞서 설정한 마스터 암호
    *   **Database**: `recipekr` (6번 단계에서 설정한 초기 DB 이름)
4. **Test Connection** 버튼을 눌러 "Connection succeeded!" 메시지가 뜨는지 확인합니다.
5. **Save**를 누르면 VSCode에서 원격 데이터베이스 관리가 가능해집니다.

---

## (선택) Spring Boot `application.yml` 수정

DB를 RDS로 교체했으므로 프로젝트의 설정 파일도 맞춰서 변경해야 합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://[RDS 엔드포인트 주소]:3306/recipekr?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
    username: [마스터 사용자 이름]
    password: [마스터 암호]
```
