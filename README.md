# acme json pasrer

The goal is to allow json processing in stream mode 

### grab it
```groovy
@Grab(group='acme.groovy', module='acmejson', version='20.04.02')
```


### classic parsing
```groovy
import groovyx.acme.json.AcmeJsonParser

def data = new AcmeJsonParser().parseText( '{"a":1,"b":2}' )
```

### replace values in json during parsing
```groovy
import groovyx.acme.json.AcmeJsonParser

def data = new AcmeJsonParser().withFilter{
		//intercept any sub-item of element with name `ddd`
		onValue('$..ddd[*]'){value,path->
			//return value is a replacement of the old one
			return value+10000
		}
		build()
	}.parseText( '[{"a":1,"b":2, "ddd":[1,2,3]}]' )
assert data[0].ddd[0]==10001
assert data[0].ddd[1]==10002
assert data[0].ddd[2]==10003
```

### replace values in json during parsing and write result into writer instead of parsing into memory
```groovy
import groovyx.acme.json.AcmeJsonParser

def w=new StringWriter()

new AcmeJsonParser().withFilter{
	//intercept any sub-item of element with name `ddd`
	onValue('$..ddd[*]'){value,path->
		//return value is a replacement of the old one
		return value+10000
	}
	write(w)
}.parseText( '[{"a":1,"b":2, "ddd":[1,2,3]}]' )

assert w.toString()=='[{"a":1,"b":2,"ddd":[10001,10002,10003]}]'
```
