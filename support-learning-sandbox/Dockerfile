FROM python:3.12-alpine

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=8080

RUN addgroup -S sandbox && adduser -S -D -H -u 10001 -G sandbox sandbox
WORKDIR /app

COPY --chown=sandbox:sandbox app.py test_app.py ./
RUN python -m unittest -v

USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD python -c "import os,urllib.request; urllib.request.urlopen('http://127.0.0.1:'+os.environ.get('PORT','8080')+'/health',timeout=2).read()"

CMD ["python", "-u", "app.py"]
