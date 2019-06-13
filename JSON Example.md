# JSON
This is a full tutorial how to build a complete parser, in this case JSON as it is simple but covers everything except reursion this API is able to do.
## Preparation
We need the official JSON syntax, which is found [here](http://json.org)
Then we will need to decide what our parser should produce, the 2 most common ways are -> deserialisation or flexible model
### Deserialisation
Means that we have POJOs and we are shure the JSON follows the structure of the POJO and fill an instance of it with the JSON values via reflections. This is very very usefull and every professional Parser has such a feature but it is very time consuming a

> Written with [StackEdit](https://stackedit.io/).
<!--stackedit_data:
eyJoaXN0b3J5IjpbMzMyNjcyMjA2XX0=
-->