import Link from "next/link";
import { FileText, ArrowRightLeft, FileSearch } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const workflows = [
  {
    href: "/claims",
    title: "Claims Generation",
    badge: "EDI 837P",
    description: "Generate professional claims from patient encounters.",
    detail: "Submit encounter IDs to generate downloadable EDI claim files.",
    icon: FileText,
    primary: true,
  },
  {
    href: "/eligibility-request",
    title: "Eligibility Request",
    badge: "EDI 270",
    description: "Create eligibility inquiry files for insurance verification.",
    detail: "Submit patient IDs to generate downloadable eligibility inquiry files.",
    icon: ArrowRightLeft,
    primary: false,
  },
  {
    href: "/eligibility-response",
    title: "Eligibility Response",
    badge: "EDI 271",
    description: "Parse and view eligibility response data.",
    detail: "Upload EDI 271 files to extract and display eligibility details.",
    icon: FileSearch,
    primary: false,
  },
];

export default function Dashboard() {
  return (
    <div>
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          EDI healthcare transaction management system.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {workflows.map((wf) => (
          <Card key={wf.href}>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-semibold">{wf.title}</CardTitle>
                <Badge variant="outline">{wf.badge}</Badge>
              </div>
              <CardDescription>{wf.description}</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-xs text-muted-foreground">{wf.detail}</p>
            </CardContent>
            <CardFooter>
              <Button
                render={<Link href={wf.href} />}
                variant={wf.primary ? "default" : "outline"}
                className="w-full"
              >
                <wf.icon className="mr-2 h-4 w-4" />
                {wf.title} &rarr;
              </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </div>
  );
}
