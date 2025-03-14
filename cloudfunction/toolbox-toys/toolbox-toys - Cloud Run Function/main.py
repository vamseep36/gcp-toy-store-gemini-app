import functions_framework
from toolbox_langchain import ToolboxClient

@functions_framework.http
def hello_http(request):
    """HTTP Cloud Function.
    Args:
        request (flask.Request): The request object.
        <https://flask.palletsprojects.com/en/1.1.x/api/#incoming-request-data>
    Returns:
        The response text, or any set of values that can be turned into a
        Response object using `make_response`
        <https://flask.palletsprojects.com/en/1.1.x/api/#flask.make_response>.
    """
    request_json = request.get_json(silent=True)
    request_args = request.args

    

    if request_json and 'name' in request_json:
        name = request_json['name']
    elif request_args and 'name' in request_args:
        name = request_args['name']
    else:
        name = 'World'
    price = toolboxcall(name);
    return price

def toolboxcall(searchText):
  toolbox = ToolboxClient("https://toolbox-uxu5wi2jpa-uc.a.run.app")
  tool = toolbox.load_tool("get-toy-price")
  print(tool)
  result = tool.invoke({"description": searchText})
  print(result.get('result').split(':')[1].replace('}]',''))
  priceOutput = 'Price: $' + result.get('result').split(':')[1].replace('}]','')
  return priceOutput
