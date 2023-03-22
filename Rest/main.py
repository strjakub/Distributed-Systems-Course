import asyncio
import datetime
import requests
from fastapi import FastAPI, Request, Form
from fastapi.responses import RedirectResponse
from fastapi.templating import Jinja2Templates
from starlette import status

app = FastAPI()
templates = Jinja2Templates(directory="")
countries = {'USA': 'united-states',
             'POL': 'poland',
             'ESP': 'spain',
             'DEU': 'germany',
             'HRV': 'croatia',
             'FRA': 'france',
             'ITA': 'italy',
             'JPN': 'japan',
             'GBR': 'united-kingdom',
             'NOR': 'norway',
             'IRL': 'ireland',
             'IND': 'india',
             'FIN': 'finland',
             'SWE': 'sweden',
             'BRA': 'brazil'}
user_db = {'test': 'test'}
logged = False


@app.get("/")
async def root():
    return RedirectResponse("/login", status_code=status.HTTP_308_PERMANENT_REDIRECT)


@app.get("/login")
async def login_page(request: Request):
    if logged:
        return RedirectResponse("/form", status_code=status.HTTP_307_TEMPORARY_REDIRECT)
    return templates.TemplateResponse('login.html',
                                      context={'request': request, 'result': ""},
                                      status_code=status.HTTP_200_OK)


@app.get("/form")
async def form_display(request: Request):
    if not logged:
        return RedirectResponse("/login", status_code=status.HTTP_307_TEMPORARY_REDIRECT)
    return templates.TemplateResponse('form.html',
                                      context={'request': request, 'result': ""},
                                      status_code=status.HTTP_200_OK)


@app.post("/login")
async def logging_in(request: Request, login: str = Form(...), password: str = Form(...)):
    global logged
    if login not in user_db.keys() or user_db[login] != password:
        return templates.TemplateResponse('login.html',
                                          context={'request': request, 'result': "wrong credentials"},
                                          status_code=status.HTTP_401_UNAUTHORIZED)
    logged = True
    return RedirectResponse("/form", status_code=status.HTTP_302_FOUND)


@app.post("/form")
async def form_submit(request: Request, date: str = Form(...), cc: str = Form(...), period: int = Form(...)):
    period = min(max(period, 1), 10)
    data = await get_data(date, period, cc)
    return data_template(request, data)


async def get_data_from_common_covid_api(date: str, cc: str, period: int):
    cases = []
    for i in range(period):
        load = requests.get('https://covid-api.com/api/reports/total?date=' + date + '&iso=' + cc.upper())
        if not load.ok or len(load.json().get("data")) == 0:
            return None
        load_json = load.json().get("data")
        cases.append((load_json.get("date"), load_json.get("confirmed")))
        date = get_date_after_certain_period(date, 1)
    return cases


async def get_data_from_covid19api(date: str, next_date: str, cc: str):
    try:
        load = requests.get('https://api.covid19api.com/total/country/' +
                            countries[cc.upper()] +
                            '/status/confirmed?from=' +
                            date +
                            'T00:00:00Z&to=' +
                            next_date +
                            'T23:59:59Z')
    except KeyError:
        return None
    if not load.ok:
        return None
    cases = []
    for i in load.json():
        cases.append((i.get("Date").split('T')[0], i.get("Cases")))
    return cases


async def get_data(date: str, period: int, cc: str):
    next_date = get_date_after_certain_period(date, period - 1)
    task_first = asyncio.create_task(get_data_from_common_covid_api(date, cc, period))
    task_second = asyncio.create_task(get_data_from_covid19api(date, next_date, cc))
    mean_cases = None
    max_increase = None
    cases_first = await task_first
    cases_second = await task_second
    if cases_first is not None and cases_second is not None:
        mean_cases = []
        for i in range(len(cases_first)):
            one_day_cases_mean = (cases_first[i][0], (int(cases_first[i][1]) + int(cases_second[i][1])) // 2)
            mean_cases.append(one_day_cases_mean)
        if period > 1:
            max_increase = mean_cases[1][1] - mean_cases[0][1]
        for i in range(1, period - 1):
            if max_increase < mean_cases[i + 1][1] - mean_cases[i][1]:
                max_increase = mean_cases[i + 1][1] - mean_cases[i][1]
    return cases_first, cases_second, mean_cases, max_increase


def get_date_after_certain_period(date: str, period: int):
    return (datetime.datetime.strptime(date, "%Y-%m-%d").date()
            + datetime.timedelta(days=period)) \
        .strftime("%Y-%m-%d")


def data_template(request, data):
    if data[0] is None and data[1] is None:
        return templates.TemplateResponse('form.html',
                                          context={'request': request,
                                                   'result1': "service unavailable",
                                                   'result2': "service unavailable",
                                                   'result_mean': data[2],
                                                   'max_increase': data[3]},
                                          status_code=status.HTTP_503_SERVICE_UNAVAILABLE)
    elif data[0] is None and data[1] is not None:
        return templates.TemplateResponse('form.html',
                                          context={'request': request,
                                                   'result1': "service unavailable",
                                                   'result2': data[1],
                                                   'result_mean': data[2],
                                                   'max_increase': data[3]},
                                          status_code=status.HTTP_206_PARTIAL_CONTENT)
    elif data[0] is not None and data[1] is None:
        return templates.TemplateResponse('form.html',
                                          context={'request': request,
                                                   'result1': data[0],
                                                   'result2': "service unavailable",
                                                   'result_mean': data[2],
                                                   'max_increase': data[3]},
                                          status_code=status.HTTP_206_PARTIAL_CONTENT)
    else:
        return templates.TemplateResponse('form.html',
                                          context={'request': request,
                                                   'result1': data[0],
                                                   'result2': data[1],
                                                   'result_mean': data[2],
                                                   'max_increase': data[3]},
                                          status_code=status.HTTP_200_OK)
