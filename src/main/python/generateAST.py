from glob import glob
from pathlib import Path
import sys

import json

ASTTemplate = """package com.Jlox;

abstract class {baseName} {{

    {visitorInterface}

    abstract <R> R accept(Visitor<R> visitor);

    {body}

}}
"""

visitorTemplate = """interface Visitor<R> {{
        {visitorBody}
}}
"""


SubClassTemplate = """
static class {className} extends {baseName} {{

    {FIELDS}

    {className}({args}) {{
        {initBody}
    }}

    @Override
    <R> R accept(Visitor<R> visitor) {{
        return visitor.visit{className}{baseName}(this);
    }}
}}
"""


def _nlJoin(method, inList, n_spaces=4, space=' '):
    sep = f"\n{n_spaces*space}"
    return sep.join([method(_) for _ in inList])


def getSubClass(baseName, sbc):
    className = sbc["className"]
    Fields = sbc["Fields"]
    baseName = baseName

    FIELDS = _nlJoin(lambda x: f"final {x[0]} {x[1]};", Fields)
    args = ", ".join([f"{x[0]} {x[1]}" for x in Fields])
    initBody = _nlJoin(lambda x: f"this.{x[1]} = {x[1]};", Fields)

    return SubClassTemplate.format(className=className, baseName=baseName, FIELDS=FIELDS, args=args, initBody=initBody,)

def getVisitor(baseName, sbc):
    visitorBody = _nlJoin(lambda x: f"R visit{x['className']}{baseName}({x['className']} {baseName.lower()});", sbc)
    return visitorTemplate.format(visitorBody=visitorBody)

def generateAST(baseName: str, subClasses: list):
    body = _nlJoin(lambda x: getSubClass(baseName, x), subClasses)
    visitorInterface = getVisitor(baseName=baseName, sbc=subClasses)
    return ASTTemplate.format(baseName=baseName, body=body, visitorInterface=visitorInterface)

def getJSON(fileName) -> str:
    baseName = Path(fileName).stem
    with open(fileName) as inFile:
        subClasses = json.load(inFile)

    return generateAST(baseName=baseName, subClasses=subClasses)        


if __name__ == "__main__":
    resourceDir, outDir = sys.argv[1:3]
    for fileName in Path(resourceDir).glob("*json"):
        outPath = Path(outDir, f"{fileName.stem}.java")
        with open(outPath, "w") as outFile:
            print(getJSON(fileName), file=outFile)
