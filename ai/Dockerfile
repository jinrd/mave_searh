# Python 3.9 버전의 가벼운 이미지를 기반으로 시작한다.
FROM python:3.9-slim-buster

# 작업 디렉토리를 /app 으로 설정한다.
WORKDIR /app

# Python 의존성 목록 파일을 현재 디렉토리로 복사한다.
COPY requirements.txt .

# requirements.txt 에 명시된 모든 Python 패키지를 설치한다.
RUN pip install -r requirements.txt

# python 서버 코드(ai_server.py) 를 현재 디렉토리로 복사한다.
COPY ai_server.py .

# 컨테이너의 8000 번 포트를 노출한다.
EXPOSE 8000

# 컨테이너가 시작될 때 실행될 명령어를 설정한다.
ENTRYPOINT ["python" , "ai_server.py"]