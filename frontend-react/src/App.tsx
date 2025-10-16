import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { useState } from "react"
import { Sparkles, Zap, Trophy, TrendingUp, Star, Rocket } from "lucide-react"
import ShazamUI from "./pages/homepage"

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <ShazamUI/>
    </>
  )
}

export default App