const pptxgen = require("pptxgenjs");
const React = require("react");
const ReactDOMServer = require("react-dom/server");
const sharp = require("sharp");
const {
  FaChartLine, FaUserPlus, FaWallet, FaExchangeAlt, FaShieldAlt,
  FaClock, FaTelegramPlane, FaCheckCircle, FaRocket, FaGlobe,
  FaMoneyBillWave, FaUsers, FaArrowRight, FaBitcoin, FaLock
} = require("react-icons/fa");

function renderIconSvg(IconComponent, color = "#000000", size = 256) {
  return ReactDOMServer.renderToStaticMarkup(
    React.createElement(IconComponent, { color, size: String(size) })
  );
}

async function iconToBase64Png(IconComponent, color, size = 256) {
  const svg = renderIconSvg(IconComponent, color, size);
  const pngBuffer = await sharp(Buffer.from(svg)).png().toBuffer();
  return "image/png;base64," + pngBuffer.toString("base64");
}

// Helper: fresh shadow each time (pitfall #7)
const makeShadow = () => ({ type: "outer", color: "000000", blur: 8, offset: 2, angle: 135, opacity: 0.12 });
const makeCardShadow = () => ({ type: "outer", color: "000000", blur: 10, offset: 3, angle: 135, opacity: 0.10 });

// Colors
const C = {
  navy: "0A1628",
  darkBlue: "0F2345",
  blue: "1A6BF5",
  lightBlue: "3B82F6",
  cyan: "06B6D4",
  teal: "0D9488",
  white: "FFFFFF",
  offWhite: "F0F4F8",
  lightGray: "E2E8F0",
  gray: "94A3B8",
  darkGray: "475569",
  text: "1E293B",
  accent: "F59E0B",
  green: "10B981",
  purple: "8B5CF6",
  pink: "EC4899",
};

async function createPresentation() {
  let pres = new pptxgen();
  pres.layout = "LAYOUT_16x9";
  pres.author = "KPBridge";
  pres.title = "KPBridge - Crypto Arbitrage Platform";

  // Pre-render all icons
  const icons = {
    chart: await iconToBase64Png(FaChartLine, "#FFFFFF", 256),
    chartBlue: await iconToBase64Png(FaChartLine, "#1A6BF5", 256),
    userPlus: await iconToBase64Png(FaUserPlus, "#FFFFFF", 256),
    wallet: await iconToBase64Png(FaWallet, "#FFFFFF", 256),
    walletBlue: await iconToBase64Png(FaWallet, "#1A6BF5", 256),
    exchange: await iconToBase64Png(FaExchangeAlt, "#FFFFFF", 256),
    exchangeBlue: await iconToBase64Png(FaExchangeAlt, "#1A6BF5", 256),
    shield: await iconToBase64Png(FaShieldAlt, "#FFFFFF", 256),
    shieldBlue: await iconToBase64Png(FaShieldAlt, "#1A6BF5", 256),
    clock: await iconToBase64Png(FaClock, "#FFFFFF", 256),
    clockBlue: await iconToBase64Png(FaClock, "#1A6BF5", 256),
    telegram: await iconToBase64Png(FaTelegramPlane, "#FFFFFF", 256),
    check: await iconToBase64Png(FaCheckCircle, "#10B981", 256),
    checkWhite: await iconToBase64Png(FaCheckCircle, "#FFFFFF", 256),
    rocket: await iconToBase64Png(FaRocket, "#FFFFFF", 256),
    globe: await iconToBase64Png(FaGlobe, "#FFFFFF", 256),
    globeBlue: await iconToBase64Png(FaGlobe, "#1A6BF5", 256),
    money: await iconToBase64Png(FaMoneyBillWave, "#FFFFFF", 256),
    users: await iconToBase64Png(FaUsers, "#FFFFFF", 256),
    arrow: await iconToBase64Png(FaArrowRight, "#FFFFFF", 256),
    bitcoin: await iconToBase64Png(FaBitcoin, "#F59E0B", 256),
    bitcoinWhite: await iconToBase64Png(FaBitcoin, "#FFFFFF", 256),
    lock: await iconToBase64Png(FaLock, "#FFFFFF", 256),
  };

  // ============================================================
  // SLIDE 1: Title / Cover
  // ============================================================
  let s1 = pres.addSlide();
  s1.background = { color: C.navy };

  // Decorative circles
  s1.addShape(pres.shapes.OVAL, { x: -1.5, y: -1, w: 4, h: 4, fill: { color: C.blue, transparency: 85 } });
  s1.addShape(pres.shapes.OVAL, { x: 7.5, y: 3, w: 5, h: 5, fill: { color: C.cyan, transparency: 88 } });
  s1.addShape(pres.shapes.OVAL, { x: 4, y: -2, w: 3, h: 3, fill: { color: C.purple, transparency: 90 } });

  // Top bar accent
  s1.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 0.06, fill: { color: C.blue } });

  // Bitcoin icon
  s1.addImage({ data: icons.bitcoinWhite, x: 0.8, y: 1.2, w: 0.6, h: 0.6 });

  // Title
  s1.addText("KPBridge", {
    x: 0.8, y: 1.85, w: 8, h: 0.9,
    fontSize: 48, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Subtitle
  s1.addText([
    { text: "Crypto Kimchi Premium ", options: { color: C.cyan, bold: true } },
    { text: "Arbitrage Platform", options: { color: C.white } }
  ], { x: 0.8, y: 2.7, w: 8, h: 0.5, fontSize: 22, fontFace: "Arial", margin: 0 });

  // Tagline
  s1.addText("5\uCD08\uB9C8\uB2E4 \uC2E4\uC2DC\uAC04 \uC2DC\uC138 \uBE44\uAD50  |  \uAD6D\uB0B4 \xB7 \uD574\uC678 \uAC70\uB798\uC18C \uAE40\uD504 \uBD84\uC11D  |  \uC548\uC804\uD55C \uC790\uC0B0 \uAD00\uB9AC", {
    x: 0.8, y: 3.35, w: 8.5, h: 0.4,
    fontSize: 13, fontFace: "Arial", color: C.gray, margin: 0
  });

  // Bottom stat cards
  const statsData = [
    { num: "5\uCD08", label: "\uC2E4\uC2DC\uAC04 \uC5C5\uB370\uC774\uD2B8", icon: icons.clock },
    { num: "5+", label: "\uC9C0\uC6D0 \uAC70\uB798\uC18C", icon: icons.globe },
    { num: "24/7", label: "\uBAA8\uB2C8\uD130\uB9C1", icon: icons.chart },
  ];
  for (let i = 0; i < 3; i++) {
    const x = 0.8 + i * 2.8;
    s1.addShape(pres.shapes.RECTANGLE, {
      x, y: 4.2, w: 2.5, h: 1.0,
      fill: { color: C.darkBlue }, shadow: makeShadow()
    });
    s1.addImage({ data: statsData[i].icon, x: x + 0.2, y: 4.45, w: 0.35, h: 0.35 });
    s1.addText(statsData[i].num, {
      x: x + 0.65, y: 4.3, w: 1.6, h: 0.45,
      fontSize: 22, fontFace: "Arial Black", color: C.cyan, bold: true, margin: 0
    });
    s1.addText(statsData[i].label, {
      x: x + 0.65, y: 4.72, w: 1.6, h: 0.3,
      fontSize: 11, fontFace: "Arial", color: C.gray, margin: 0
    });
  }

  // ============================================================
  // SLIDE 2: What is Kimchi Premium?
  // ============================================================
  let s2 = pres.addSlide();
  s2.background = { color: C.offWhite };

  // Header section
  s2.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 1.3, fill: { color: C.navy } });
  s2.addText("\uAE40\uCE58\uD504\uB9AC\uBBF8\uC5C4\uC774\uB780?", {
    x: 0.7, y: 0.3, w: 8, h: 0.7,
    fontSize: 30, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Definition card
  s2.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 1.65, w: 8.6, h: 1.3,
    fill: { color: C.white }, shadow: makeCardShadow()
  });
  s2.addShape(pres.shapes.RECTANGLE, { x: 0.7, y: 1.65, w: 0.08, h: 1.3, fill: { color: C.blue } });
  s2.addText([
    { text: "\uAE40\uCE58\uD504\uB9AC\uBBF8\uC5C4(Kimchi Premium)", options: { bold: true, fontSize: 16, color: C.blue, breakLine: true } },
    { text: "\uD55C\uAD6D \uAC70\uB798\uC18C\uC758 \uC554\uD638\uD654\uD3D0 \uAC00\uACA9\uC774 \uD574\uC678 \uAC70\uB798\uC18C\uBCF4\uB2E4 \uB192\uAC8C \uD615\uC131\uB418\uB294 \uD604\uC0C1\uC785\uB2C8\uB2E4.", options: { fontSize: 13, color: C.darkGray, breakLine: true } },
    { text: "\uC774 \uAC00\uACA9 \uCC28\uC774\uB97C \uC774\uC6A9\uD558\uBA74 \uC800\uC704\uD5D8 \uCC28\uC775\uAC70\uB798\uAC00 \uAC00\uB2A5\uD569\uB2C8\uB2E4.", options: { fontSize: 13, color: C.darkGray } }
  ], { x: 1.1, y: 1.75, w: 7.9, h: 1.1, valign: "middle", margin: 0 });

  // Example visualization
  s2.addText("\uC608\uC2DC: BTC \uAC00\uACA9 \uBE44\uAD50", {
    x: 0.7, y: 3.25, w: 4, h: 0.35,
    fontSize: 13, fontFace: "Arial", color: C.darkGray, bold: true, margin: 0
  });

  // Korean exchange card
  s2.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 3.7, w: 3.8, h: 1.5,
    fill: { color: C.white }, shadow: makeCardShadow()
  });
  s2.addShape(pres.shapes.RECTANGLE, { x: 0.7, y: 3.7, w: 3.8, h: 0.4, fill: { color: C.blue } });
  s2.addText("\uD55C\uAD6D \uAC70\uB798\uC18C (Upbit)", {
    x: 0.7, y: 3.72, w: 3.8, h: 0.4,
    fontSize: 12, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });
  s2.addText("1 BTC = 138,500,000 KRW", {
    x: 0.7, y: 4.25, w: 3.8, h: 0.5,
    fontSize: 16, fontFace: "Arial Black", color: C.text, bold: true, align: "center", valign: "middle", margin: 0
  });
  s2.addText("\uAD6D\uB0B4 \uC218\uC694 \uACFC\uC5F4\uB85C \uB192\uC740 \uAC00\uACA9", {
    x: 0.7, y: 4.7, w: 3.8, h: 0.35,
    fontSize: 10, fontFace: "Arial", color: C.gray, align: "center", valign: "middle", margin: 0
  });

  // Arrow between
  s2.addShape(pres.shapes.RECTANGLE, {
    x: 4.65, y: 4.1, w: 0.7, h: 0.6,
    fill: { color: C.accent }
  });
  s2.addText("\uCC28\uC775", {
    x: 4.65, y: 4.1, w: 0.7, h: 0.6,
    fontSize: 11, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });

  // Foreign exchange card
  s2.addShape(pres.shapes.RECTANGLE, {
    x: 5.5, y: 3.7, w: 3.8, h: 1.5,
    fill: { color: C.white }, shadow: makeCardShadow()
  });
  s2.addShape(pres.shapes.RECTANGLE, { x: 5.5, y: 3.7, w: 3.8, h: 0.4, fill: { color: C.teal } });
  s2.addText("\uD574\uC678 \uAC70\uB798\uC18C (OKX)", {
    x: 5.5, y: 3.72, w: 3.8, h: 0.4,
    fontSize: 12, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });
  s2.addText("1 BTC = 131,200,000 KRW", {
    x: 5.5, y: 4.25, w: 3.8, h: 0.5,
    fontSize: 16, fontFace: "Arial Black", color: C.text, bold: true, align: "center", valign: "middle", margin: 0
  });
  s2.addText("\uAD6D\uC81C \uC2DC\uC138 \uAE30\uC900 \uAC00\uACA9", {
    x: 5.5, y: 4.7, w: 3.8, h: 0.35,
    fontSize: 10, fontFace: "Arial", color: C.gray, align: "center", valign: "middle", margin: 0
  });

  // Premium result
  s2.addShape(pres.shapes.RECTANGLE, {
    x: 2.5, y: 5.25, w: 5, h: 0.25,
    fill: { color: C.accent, transparency: 80 }
  });
  s2.addText([
    { text: "\uAE40\uCE58\uD504\uB9AC\uBBF8\uC5C4: ", options: { bold: true, color: C.text } },
    { text: "+5.56%", options: { bold: true, color: "DC2626" } },
    { text: "  \u2192  \uCC28\uC775: \uC57D 7,300,000 KRW / BTC", options: { color: C.darkGray } }
  ], {
    x: 1.2, y: 5.2, w: 7.6, h: 0.35,
    fontSize: 13, fontFace: "Arial", align: "center", valign: "middle", margin: 0
  });

  // ============================================================
  // SLIDE 3: KPBridge Advantages
  // ============================================================
  let s3 = pres.addSlide();
  s3.background = { color: C.offWhite };

  s3.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 1.3, fill: { color: C.navy } });
  s3.addText("KPBridge\uB9CC\uC758 \uD575\uC2EC \uC7A5\uC810", {
    x: 0.7, y: 0.3, w: 8, h: 0.7,
    fontSize: 30, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // 2x2 advantage cards
  const advantages = [
    {
      icon: icons.clockBlue, title: "\uC2E4\uC2DC\uAC04 5\uCD08 \uC5C5\uB370\uC774\uD2B8",
      desc: "\uAD6D\uB0B4\uC678 \uAC70\uB798\uC18C \uC2DC\uC138\uB97C 5\uCD08\uB9C8\uB2E4 \uC790\uB3D9 \uAC31\uC2E0\uD558\uC5EC \uD56D\uC0C1 \uCD5C\uC2E0 \uAE40\uD504\uC728\uC744 \uD655\uC778\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."
    },
    {
      icon: icons.globeBlue, title: "5\uAC1C \uAC70\uB798\uC18C \uC9C0\uC6D0",
      desc: "\uC5C5\uBE44\uD2B8, \uBE57\uC378 / OKX, HTX, Gate.io \uB4F1 \uAD6D\uB0B4\uC678 \uC8FC\uC694 \uAC70\uB798\uC18C\uB97C \uC790\uC720\uB86D\uAC8C \uC120\uD0DD \uBE44\uAD50\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."
    },
    {
      icon: icons.walletBlue, title: "\uC774\uC911 \uC9C0\uAC11 \uC2DC\uC2A4\uD15C",
      desc: "\uD604\uAE08 \uC790\uC0B0(\uD3EC\uC778\uD2B8)\uACFC \uCF54\uC778 \uC790\uC0B0\uC744 \uBD84\uB9AC \uAD00\uB9AC\uD558\uC5EC \uD22C\uBA85\uD558\uACE0 \uC548\uC804\uD55C \uC790\uC0B0 \uC6B4\uC6A9\uC744 \uBCF4\uC7A5\uD569\uB2C8\uB2E4."
    },
    {
      icon: icons.shieldBlue, title: "\uAD00\uB9AC\uC790 \uC2B9\uC778 \uC2DC\uC2A4\uD15C",
      desc: "\uCDA9\uC804/\uCD9C\uAE08 \uC2E0\uCCAD \uC2DC \uAD00\uB9AC\uC790 \uC2B9\uC778\uC744 \uAC70\uCCD0 \uCC98\uB9AC\uB418\uBA70, \uD154\uB808\uADF8\uB7A8 \uC2E4\uC2DC\uAC04 \uC54C\uB9BC\uC73C\uB85C \uBE60\uB978 \uB300\uC751\uC774 \uAC00\uB2A5\uD569\uB2C8\uB2E4."
    },
  ];

  for (let i = 0; i < 4; i++) {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.7 + col * 4.5;
    const y = 1.7 + row * 1.85;

    s3.addShape(pres.shapes.RECTANGLE, {
      x, y, w: 4.1, h: 1.55,
      fill: { color: C.white }, shadow: makeCardShadow()
    });

    // Icon circle
    s3.addShape(pres.shapes.OVAL, {
      x: x + 0.25, y: y + 0.35, w: 0.7, h: 0.7,
      fill: { color: C.blue, transparency: 88 }
    });
    s3.addImage({ data: advantages[i].icon, x: x + 0.37, y: y + 0.47, w: 0.45, h: 0.45 });

    s3.addText(advantages[i].title, {
      x: x + 1.15, y: y + 0.2, w: 2.7, h: 0.4,
      fontSize: 15, fontFace: "Arial", color: C.text, bold: true, margin: 0
    });
    s3.addText(advantages[i].desc, {
      x: x + 1.15, y: y + 0.6, w: 2.7, h: 0.75,
      fontSize: 11, fontFace: "Arial", color: C.darkGray, margin: 0
    });
  }

  // ============================================================
  // SLIDE 4: How to Use - Step 1 (Registration & Login)
  // ============================================================
  let s4 = pres.addSlide();
  s4.background = { color: C.offWhite };

  s4.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 1.3, fill: { color: C.navy } });
  s4.addText([
    { text: "STEP 1  ", options: { color: C.cyan, bold: true } },
    { text: "\uD68C\uC6D0\uAC00\uC785 & \uB85C\uADF8\uC778", options: { color: C.white } }
  ], {
    x: 0.7, y: 0.3, w: 8, h: 0.7,
    fontSize: 28, fontFace: "Arial Black", bold: true, margin: 0
  });

  // Left: Registration info
  s4.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 1.65, w: 4.1, h: 3.6,
    fill: { color: C.white }, shadow: makeCardShadow()
  });
  s4.addShape(pres.shapes.RECTANGLE, { x: 0.7, y: 1.65, w: 4.1, h: 0.5, fill: { color: C.blue } });
  s4.addImage({ data: icons.userPlus, x: 0.9, y: 1.72, w: 0.3, h: 0.3 });
  s4.addText("\uD68C\uC6D0\uAC00\uC785", {
    x: 1.3, y: 1.65, w: 3, h: 0.5,
    fontSize: 15, fontFace: "Arial", color: C.white, bold: true, valign: "middle", margin: 0
  });

  const regFields = [
    "\uC544\uC774\uB514 / \uBE44\uBC00\uBC88\uD638 \uC124\uC815",
    "\uC774\uB984 / \uC5F0\uB77D\uCC98 / \uC774\uBA54\uC77C \uC785\uB825",
    "\uC0DD\uB144\uC6D4\uC77C \uC785\uB825",
    "\uCD94\uCC9C\uC778 \uCF54\uB4DC \uC785\uB825 (\uC120\uD0DD)",
  ];
  for (let i = 0; i < regFields.length; i++) {
    s4.addImage({ data: icons.check, x: 1.0, y: 2.35 + i * 0.55, w: 0.22, h: 0.22 });
    s4.addText(regFields[i], {
      x: 1.35, y: 2.3 + i * 0.55, w: 3.2, h: 0.35,
      fontSize: 12, fontFace: "Arial", color: C.text, valign: "middle", margin: 0
    });
  }

  // Bonus callout
  s4.addShape(pres.shapes.RECTANGLE, {
    x: 1.0, y: 4.55, w: 3.5, h: 0.5,
    fill: { color: C.accent, transparency: 85 }
  });
  s4.addText([
    { text: "\uAC00\uC785 \uBCF4\uB108\uC2A4: ", options: { bold: true, color: C.accent } },
    { text: "150 KP \uC989\uC2DC \uC9C0\uAE09!", options: { color: C.text } }
  ], {
    x: 1.1, y: 4.55, w: 3.3, h: 0.5,
    fontSize: 13, fontFace: "Arial", valign: "middle", margin: 0
  });

  // Right: Login & Benefits
  s4.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: 1.65, w: 4.1, h: 3.6,
    fill: { color: C.white }, shadow: makeCardShadow()
  });
  s4.addShape(pres.shapes.RECTANGLE, { x: 5.2, y: 1.65, w: 4.1, h: 0.5, fill: { color: C.teal } });
  s4.addImage({ data: icons.lock, x: 5.4, y: 1.72, w: 0.3, h: 0.3 });
  s4.addText("\uB85C\uADF8\uC778 \uD6C4 \uC774\uC6A9 \uAC00\uB2A5 \uAE30\uB2A5", {
    x: 5.8, y: 1.65, w: 3, h: 0.5,
    fontSize: 15, fontFace: "Arial", color: C.white, bold: true, valign: "middle", margin: 0
  });

  const loginBenefits = [
    "\uC2E4\uC2DC\uAC04 \uC790\uC0B0 \uD604\uD669 \uD655\uC778",
    "\uCDA9\uC804 / \uCD9C\uAE08 \uC2E0\uCCAD",
    "\uAE40\uD504 \uCC28\uC775\uAC70\uB798 \uC8FC\uBB38",
    "\uAC70\uB798 \uB0B4\uC5ED \uC870\uD68C",
    "\uCD94\uCC9C\uC778 \uCF54\uB4DC \uBCF4\uB108\uC2A4 (50 KP)",
  ];
  for (let i = 0; i < loginBenefits.length; i++) {
    s4.addImage({ data: icons.check, x: 5.5, y: 2.35 + i * 0.5, w: 0.22, h: 0.22 });
    s4.addText(loginBenefits[i], {
      x: 5.85, y: 2.3 + i * 0.5, w: 3.2, h: 0.35,
      fontSize: 12, fontFace: "Arial", color: C.text, valign: "middle", margin: 0
    });
  }

  // ============================================================
  // SLIDE 5: How to Use - Step 2 (Real-time Prices)
  // ============================================================
  let s5 = pres.addSlide();
  s5.background = { color: C.offWhite };

  s5.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 1.3, fill: { color: C.navy } });
  s5.addText([
    { text: "STEP 2  ", options: { color: C.cyan, bold: true } },
    { text: "\uC2E4\uC2DC\uAC04 \uC2DC\uC138 \uD655\uC778 & \uAE40\uD504 \uBD84\uC11D", options: { color: C.white } }
  ], {
    x: 0.7, y: 0.3, w: 9, h: 0.7,
    fontSize: 28, fontFace: "Arial Black", bold: true, margin: 0
  });

  // Main dashboard description card
  s5.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 1.65, w: 8.6, h: 1.2,
    fill: { color: C.white }, shadow: makeCardShadow()
  });
  s5.addShape(pres.shapes.RECTANGLE, { x: 0.7, y: 1.65, w: 0.08, h: 1.2, fill: { color: C.cyan } });
  s5.addText([
    { text: "\uBA54\uC778 \uB300\uC2DC\uBCF4\uB4DC\uC5D0\uC11C \uAD6D\uB0B4/\uD574\uC678 \uAC70\uB798\uC18C\uB97C \uC120\uD0DD\uD558\uBA74", options: { fontSize: 13, color: C.text, breakLine: true } },
    { text: "BTC, ETH \uB4F1 \uC8FC\uC694 \uCF54\uC778\uC758 \uD604\uC7AC\uAC00, \uAE40\uCE58\uD504\uB9AC\uBBF8\uC5C4(%), \uB9E4\uC218/\uB9E4\uB3C4 \uAC00\uACA9\uC744 \uD55C\uB208\uC5D0 \uD655\uC778\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4.", options: { fontSize: 13, color: C.darkGray } }
  ], { x: 1.1, y: 1.7, w: 7.9, h: 1.1, valign: "middle", margin: 0 });

  // Simulated price table
  s5.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 3.15, w: 8.6, h: 2.3,
    fill: { color: C.white }, shadow: makeCardShadow()
  });

  // Table header
  s5.addShape(pres.shapes.RECTANGLE, { x: 0.7, y: 3.15, w: 8.6, h: 0.45, fill: { color: C.navy } });
  const headers = ["\uCF54\uC778", "\uD604\uC7AC\uAC00 (KRW)", "\uAE40\uD504\uC728", "\uB9E4\uC218\uAC00 (USDT)", "\uB9E4\uB3C4\uAC00 (USDT)"];
  const colX = [0.8, 2.3, 4.8, 6.0, 7.8];
  const colW = [1.4, 2.3, 1.1, 1.7, 1.5];
  for (let i = 0; i < headers.length; i++) {
    s5.addText(headers[i], {
      x: colX[i], y: 3.15, w: colW[i], h: 0.45,
      fontSize: 10, fontFace: "Arial", color: C.white, bold: true, valign: "middle", margin: 0
    });
  }

  // BTC row
  const btcData = ["BTC", "138,500,000", "+5.56%", "95,240.50", "95,180.20"];
  for (let i = 0; i < btcData.length; i++) {
    let clr = C.text;
    if (i === 2) clr = "DC2626";
    if (i === 0) {
      s5.addText(btcData[i], {
        x: colX[i], y: 3.65, w: colW[i], h: 0.45,
        fontSize: 12, fontFace: "Arial", color: C.accent, bold: true, valign: "middle", margin: 0
      });
    } else {
      s5.addText(btcData[i], {
        x: colX[i], y: 3.65, w: colW[i], h: 0.45,
        fontSize: 12, fontFace: "Arial", color: clr, bold: i === 2, valign: "middle", margin: 0
      });
    }
  }

  // ETH row
  s5.addShape(pres.shapes.RECTANGLE, { x: 0.7, y: 4.15, w: 8.6, h: 0.45, fill: { color: C.offWhite } });
  const ethData = ["ETH", "4,850,000", "+3.21%", "3,320.80", "3,318.50"];
  for (let i = 0; i < ethData.length; i++) {
    let clr = C.text;
    if (i === 2) clr = "DC2626";
    if (i === 0) {
      s5.addText(ethData[i], {
        x: colX[i], y: 4.15, w: colW[i], h: 0.45,
        fontSize: 12, fontFace: "Arial", color: C.purple, bold: true, valign: "middle", margin: 0
      });
    } else {
      s5.addText(ethData[i], {
        x: colX[i], y: 4.15, w: colW[i], h: 0.45,
        fontSize: 12, fontFace: "Arial", color: clr, bold: i === 2, valign: "middle", margin: 0
      });
    }
  }

  // Exchange selectors note
  s5.addText([
    { text: "\uAD6D\uB0B4: ", options: { bold: true, color: C.blue } },
    { text: "Upbit \xB7 Bithumb    ", options: { color: C.text } },
    { text: "\uD574\uC678: ", options: { bold: true, color: C.teal } },
    { text: "OKX \xB7 HTX \xB7 Gate.io", options: { color: C.text } },
    { text: "    \uC790\uC720\uB86D\uAC8C \uC870\uD569 \uC120\uD0DD \uAC00\uB2A5!", options: { color: C.gray, italic: true } }
  ], {
    x: 0.7, y: 4.7, w: 8.6, h: 0.45,
    fontSize: 11, fontFace: "Arial", align: "center", valign: "middle", margin: 0
  });

  // ============================================================
  // SLIDE 6: How to Use - Step 3 (Asset Management)
  // ============================================================
  let s6 = pres.addSlide();
  s6.background = { color: C.offWhite };

  s6.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 1.3, fill: { color: C.navy } });
  s6.addText([
    { text: "STEP 3  ", options: { color: C.cyan, bold: true } },
    { text: "\uC790\uC0B0 \uAD00\uB9AC & \uCDA9\uC804/\uCD9C\uAE08", options: { color: C.white } }
  ], {
    x: 0.7, y: 0.3, w: 9, h: 0.7,
    fontSize: 28, fontFace: "Arial Black", bold: true, margin: 0
  });

  // Cash wallet card
  s6.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 1.65, w: 4.1, h: 2.2,
    fill: { color: C.blue }
  });
  s6.addImage({ data: icons.money, x: 1.0, y: 1.85, w: 0.4, h: 0.4 });
  s6.addText("\uD604\uAE08 \uC790\uC0B0 (\uD3EC\uC778\uD2B8)", {
    x: 1.5, y: 1.8, w: 3, h: 0.45,
    fontSize: 16, fontFace: "Arial", color: C.white, bold: true, valign: "middle", margin: 0
  });
  s6.addText("152,481 KRW", {
    x: 1.0, y: 2.35, w: 3.5, h: 0.55,
    fontSize: 28, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });
  s6.addShape(pres.shapes.RECTANGLE, {
    x: 1.0, y: 3.05, w: 1.5, h: 0.45,
    fill: { color: C.white, transparency: 70 }
  });
  s6.addText("\uCDA9\uC804\uD558\uAE30", {
    x: 1.0, y: 3.05, w: 1.5, h: 0.45,
    fontSize: 12, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });
  s6.addShape(pres.shapes.RECTANGLE, {
    x: 2.7, y: 3.05, w: 1.5, h: 0.45,
    fill: { color: C.white, transparency: 70 }
  });
  s6.addText("\uCD9C\uAE08\uD558\uAE30", {
    x: 2.7, y: 3.05, w: 1.5, h: 0.45,
    fontSize: 12, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });

  // Coin wallet card
  s6.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: 1.65, w: 4.1, h: 2.2,
    fill: { color: C.purple }
  });
  s6.addImage({ data: icons.bitcoinWhite, x: 5.5, y: 1.85, w: 0.4, h: 0.4 });
  s6.addText("\uCF54\uC778 \uC790\uC0B0", {
    x: 6.0, y: 1.8, w: 3, h: 0.45,
    fontSize: 16, fontFace: "Arial", color: C.white, bold: true, valign: "middle", margin: 0
  });
  s6.addText("0 KRW", {
    x: 5.5, y: 2.35, w: 3.5, h: 0.55,
    fontSize: 28, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });
  s6.addShape(pres.shapes.RECTANGLE, {
    x: 5.5, y: 3.05, w: 1.5, h: 0.45,
    fill: { color: C.white, transparency: 70 }
  });
  s6.addText("\uCDA9\uC804\uD558\uAE30", {
    x: 5.5, y: 3.05, w: 1.5, h: 0.45,
    fontSize: 12, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });
  s6.addShape(pres.shapes.RECTANGLE, {
    x: 7.2, y: 3.05, w: 1.5, h: 0.45,
    fill: { color: C.white, transparency: 70 }
  });
  s6.addText("\uCD9C\uAE08\uD558\uAE30", {
    x: 7.2, y: 3.05, w: 1.5, h: 0.45,
    fontSize: 12, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });

  // Process flow
  s6.addText("\uCDA9\uC804/\uCD9C\uAE08 \uCC98\uB9AC \uD504\uB85C\uC138\uC2A4", {
    x: 0.7, y: 4.15, w: 8, h: 0.35,
    fontSize: 14, fontFace: "Arial", color: C.text, bold: true, margin: 0
  });

  const steps = [
    { label: "\uC2E0\uCCAD", color: C.blue },
    { label: "\uD154\uB808\uADF8\uB7A8 \uC54C\uB9BC", color: "0EA5E9" },
    { label: "\uAD00\uB9AC\uC790 \uC2B9\uC778", color: C.teal },
    { label: "\uC794\uC561 \uBC18\uC601", color: C.green },
  ];
  for (let i = 0; i < steps.length; i++) {
    const sx = 0.7 + i * 2.35;
    s6.addShape(pres.shapes.RECTANGLE, {
      x: sx, y: 4.65, w: 1.9, h: 0.7,
      fill: { color: steps[i].color }
    });
    s6.addText(steps[i].label, {
      x: sx, y: 4.65, w: 1.9, h: 0.7,
      fontSize: 13, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
    });
    if (i < 3) {
      s6.addImage({ data: icons.arrow, x: sx + 1.95, y: 4.82, w: 0.3, h: 0.3 });
    }
  }

  // ============================================================
  // SLIDE 7: How to Use - Step 4 (Trading & History)
  // ============================================================
  let s7 = pres.addSlide();
  s7.background = { color: C.offWhite };

  s7.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 10, h: 1.3, fill: { color: C.navy } });
  s7.addText([
    { text: "STEP 4  ", options: { color: C.cyan, bold: true } },
    { text: "\uAC70\uB798 \uC2E4\uD589 & \uB0B4\uC5ED \uD655\uC778", options: { color: C.white } }
  ], {
    x: 0.7, y: 0.3, w: 9, h: 0.7,
    fontSize: 28, fontFace: "Arial Black", bold: true, margin: 0
  });

  // Trade execution card
  s7.addShape(pres.shapes.RECTANGLE, {
    x: 0.7, y: 1.65, w: 4.1, h: 3.55,
    fill: { color: C.white }, shadow: makeCardShadow()
  });
  s7.addShape(pres.shapes.RECTANGLE, { x: 0.7, y: 1.65, w: 4.1, h: 0.5, fill: { color: C.green } });
  s7.addImage({ data: icons.exchange, x: 0.9, y: 1.72, w: 0.3, h: 0.3 });
  s7.addText("\uCC28\uC775\uAC70\uB798 \uC8FC\uBB38", {
    x: 1.3, y: 1.65, w: 3, h: 0.5,
    fontSize: 15, fontFace: "Arial", color: C.white, bold: true, valign: "middle", margin: 0
  });

  const tradeSteps = [
    { num: "1", text: "\uCF54\uC778 \uC120\uD0DD (BTC / ETH)" },
    { num: "2", text: "\uB9E4\uC218 \uAC70\uB798\uC18C \uC120\uD0DD (From)" },
    { num: "3", text: "\uB9E4\uB3C4 \uAC70\uB798\uC18C \uC120\uD0DD (To)" },
    { num: "4", text: "\uD22C\uC790 \uAE08\uC561 \uC785\uB825 (KRW)" },
    { num: "5", text: "\uC8FC\uBB38 \uC2E4\uD589 \uBC84\uD2BC \uD074\uB9AD" },
  ];
  for (let i = 0; i < tradeSteps.length; i++) {
    s7.addShape(pres.shapes.OVAL, {
      x: 1.0, y: 2.35 + i * 0.5, w: 0.3, h: 0.3,
      fill: { color: C.blue }
    });
    s7.addText(tradeSteps[i].num, {
      x: 1.0, y: 2.35 + i * 0.5, w: 0.3, h: 0.3,
      fontSize: 10, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
    });
    s7.addText(tradeSteps[i].text, {
      x: 1.45, y: 2.3 + i * 0.5, w: 3.1, h: 0.35,
      fontSize: 12, fontFace: "Arial", color: C.text, valign: "middle", margin: 0
    });
  }

  // Transaction history card
  s7.addShape(pres.shapes.RECTANGLE, {
    x: 5.2, y: 1.65, w: 4.1, h: 3.55,
    fill: { color: C.white }, shadow: makeCardShadow()
  });
  s7.addShape(pres.shapes.RECTANGLE, { x: 5.2, y: 1.65, w: 4.1, h: 0.5, fill: { color: C.darkBlue } });
  s7.addText("\uAC70\uB798 \uB0B4\uC5ED (Transaction Logs)", {
    x: 5.4, y: 1.65, w: 3.7, h: 0.5,
    fontSize: 14, fontFace: "Arial", color: C.white, bold: true, valign: "middle", margin: 0
  });

  // Mini table headers
  const txHeaders = ["\uC77C\uC2DC", "\uAD6C\uBD84", "\uC9C0\uAC11", "\uAE08\uC561", "\uC0C1\uD0DC"];
  const txColX = [5.35, 6.15, 6.85, 7.5, 8.35];
  for (let i = 0; i < txHeaders.length; i++) {
    s7.addText(txHeaders[i], {
      x: txColX[i], y: 2.25, w: 0.8, h: 0.3,
      fontSize: 9, fontFace: "Arial", color: C.gray, bold: true, margin: 0
    });
  }

  // Sample rows
  const txRows = [
    ["03-12", "\uCDA9\uC804", "\uD604\uAE08", "+50,000", "\uC644\uB8CC"],
    ["03-11", "\uCD9C\uAE08", "\uCF54\uC778", "-20,000", "\uB300\uAE30"],
    ["03-10", "\uCDA9\uC804", "\uD604\uAE08", "+100,000", "\uC644\uB8CC"],
    ["03-09", "\uCDA9\uC804", "\uCF54\uC778", "+30,000", "\uC644\uB8CC"],
    ["03-08", "\uCD9C\uAE08", "\uD604\uAE08", "-10,000", "\uAC70\uC808"],
  ];
  for (let r = 0; r < txRows.length; r++) {
    if (r % 2 === 1) {
      s7.addShape(pres.shapes.RECTANGLE, { x: 5.2, y: 2.6 + r * 0.4, w: 4.1, h: 0.4, fill: { color: C.offWhite } });
    }
    for (let c = 0; c < txRows[r].length; c++) {
      let clr = C.text;
      if (c === 4 && txRows[r][c] === "\uC644\uB8CC") clr = C.green;
      if (c === 4 && txRows[r][c] === "\uB300\uAE30") clr = C.accent;
      if (c === 4 && txRows[r][c] === "\uAC70\uC808") clr = "DC2626";
      if (c === 3 && txRows[r][c].startsWith("+")) clr = C.blue;
      if (c === 3 && txRows[r][c].startsWith("-")) clr = "DC2626";
      s7.addText(txRows[r][c], {
        x: txColX[c], y: 2.6 + r * 0.4, w: 0.8, h: 0.4,
        fontSize: 9, fontFace: "Arial", color: clr, bold: c === 4, valign: "middle", margin: 0
      });
    }
  }

  // ============================================================
  // SLIDE 8: Why Join Now - Growth & Opportunity
  // ============================================================
  let s8 = pres.addSlide();
  s8.background = { color: C.navy };

  // Decorative
  s8.addShape(pres.shapes.OVAL, { x: 7.5, y: -1.5, w: 5, h: 5, fill: { color: C.blue, transparency: 88 } });
  s8.addShape(pres.shapes.OVAL, { x: -2, y: 3, w: 4, h: 4, fill: { color: C.cyan, transparency: 90 } });

  s8.addText("\uC9C0\uAE08 KPBridge\uC5D0 \uD569\uB958\uD574\uC57C \uD558\uB294 \uC774\uC720", {
    x: 0.7, y: 0.4, w: 8.6, h: 0.7,
    fontSize: 28, fontFace: "Arial Black", color: C.white, bold: true, margin: 0
  });

  // Big stat callouts
  const bigStats = [
    { num: "5\uCD08", label: "\uC2E4\uC2DC\uAC04 \uAC00\uACA9 \uAC31\uC2E0\uC73C\uB85C\n\uCD5C\uC801\uC758 \uD0C0\uC774\uBC0D \uD3EC\uCC29", color: C.cyan },
    { num: "150 KP", label: "\uAC00\uC785 \uC989\uC2DC \uBCF4\uB108\uC2A4 \uC9C0\uAE09\n\uCD94\uCC9C\uC778 \uCF54\uB4DC \uCD94\uAC00 50 KP", color: C.accent },
    { num: "24/7", label: "\uC0C1\uC2DC \uC6B4\uC601\uB418\uB294 \uD50C\uB7AB\uD3FC\n\uD154\uB808\uADF8\uB7A8 \uC989\uC2DC \uC54C\uB9BC", color: C.green },
  ];
  for (let i = 0; i < 3; i++) {
    const x = 0.7 + i * 3.1;
    s8.addShape(pres.shapes.RECTANGLE, {
      x, y: 1.5, w: 2.8, h: 1.8,
      fill: { color: C.darkBlue }, shadow: makeShadow()
    });
    s8.addText(bigStats[i].num, {
      x, y: 1.65, w: 2.8, h: 0.7,
      fontSize: 36, fontFace: "Arial Black", color: bigStats[i].color, bold: true, align: "center", valign: "middle", margin: 0
    });
    s8.addText(bigStats[i].label, {
      x, y: 2.4, w: 2.8, h: 0.7,
      fontSize: 11, fontFace: "Arial", color: C.gray, align: "center", valign: "middle", margin: 0
    });
  }

  // Key selling points
  s8.addText("\uD575\uC2EC \uD22C\uC790 \uD3EC\uC778\uD2B8", {
    x: 0.7, y: 3.7, w: 8, h: 0.4,
    fontSize: 16, fontFace: "Arial", color: C.white, bold: true, margin: 0
  });

  const sellingPoints = [
    "\uAE40\uCE58\uD504\uB9AC\uBBF8\uC5C4\uC740 \uD55C\uAD6D \uC554\uD638\uD654\uD3D0 \uC2DC\uC7A5\uC758 \uAD6C\uC870\uC801 \uD2B9\uC131 \u2014 \uC0AC\uB77C\uC9C0\uC9C0 \uC54A\uB294 \uC218\uC775 \uAE30\uD68C",
    "\uC0AC\uC6A9\uC790\uAC00 \uB9CE\uC544\uC9C8\uC218\uB85D \uAC70\uB798 \uBCFC\uB968\uC774 \uCEE4\uC838 \uC218\uC775\uB960\uC774 \uADF9\uB300\uD654",
    "\uAD00\uB9AC\uC790 \uC2B9\uC778 \uC2DC\uC2A4\uD15C\uC73C\uB85C \uC548\uC804\uD558\uACE0 \uD22C\uBA85\uD55C \uAC70\uB798 \uD658\uACBD \uBCF4\uC7A5",
    "\uD154\uB808\uADF8\uB7A8 \uC2E4\uC2DC\uAC04 \uC54C\uB9BC\uC73C\uB85C \uBE60\uB978 \uC785\uCD9C\uAE08 \uCC98\uB9AC",
  ];
  for (let i = 0; i < sellingPoints.length; i++) {
    s8.addImage({ data: icons.checkWhite, x: 0.9, y: 4.25 + i * 0.35, w: 0.2, h: 0.2 });
    s8.addText(sellingPoints[i], {
      x: 1.25, y: 4.2 + i * 0.35, w: 8, h: 0.3,
      fontSize: 12, fontFace: "Arial", color: C.lightGray, valign: "middle", margin: 0
    });
  }

  // ============================================================
  // SLIDE 9: CTA / Closing
  // ============================================================
  let s9 = pres.addSlide();
  s9.background = { color: C.navy };

  // Decorative
  s9.addShape(pres.shapes.OVAL, { x: -1, y: -1.5, w: 5, h: 5, fill: { color: C.blue, transparency: 87 } });
  s9.addShape(pres.shapes.OVAL, { x: 6, y: 2.5, w: 6, h: 6, fill: { color: C.cyan, transparency: 90 } });
  s9.addShape(pres.shapes.OVAL, { x: 3, y: -2, w: 3, h: 3, fill: { color: C.purple, transparency: 92 } });

  // Rocket icon
  s9.addImage({ data: icons.rocket, x: 4.5, y: 0.8, w: 1, h: 1 });

  // Main CTA
  s9.addText("KPBridge\uC640 \uD568\uAED8\nCrypto Arbitrage\uB97C \uC2DC\uC791\uD558\uC138\uC694", {
    x: 1, y: 1.9, w: 8, h: 1.5,
    fontSize: 36, fontFace: "Arial Black", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });

  s9.addText("\uD55C\uAD6D-\uD574\uC678 \uAC70\uB798\uC18C \uAC04 \uAE40\uCE58\uD504\uB9AC\uBBF8\uC5C4\uC744 \uC2E4\uC2DC\uAC04\uC73C\uB85C \uBD84\uC11D\uD558\uACE0\n\uC548\uC804\uD55C \uCC28\uC775\uAC70\uB798\uB97C \uACBD\uD5D8\uD574 \uBCF4\uC138\uC694", {
    x: 1.5, y: 3.35, w: 7, h: 0.8,
    fontSize: 15, fontFace: "Arial", color: C.gray, align: "center", valign: "middle", margin: 0
  });

  // CTA button shape
  s9.addShape(pres.shapes.RECTANGLE, {
    x: 3.2, y: 4.3, w: 3.6, h: 0.7,
    fill: { color: C.blue }
  });
  s9.addText("\uC9C0\uAE08 \uBC14\uB85C \uC2DC\uC791\uD558\uAE30", {
    x: 3.2, y: 4.3, w: 3.6, h: 0.7,
    fontSize: 18, fontFace: "Arial", color: C.white, bold: true, align: "center", valign: "middle", margin: 0
  });

  // Contact info
  s9.addImage({ data: icons.telegram, x: 3.8, y: 5.15, w: 0.25, h: 0.25 });
  s9.addText("Telegram \uC9C0\uC6D0  |  24\uC2DC\uAC04 \uC6B4\uC601", {
    x: 4.1, y: 5.13, w: 3, h: 0.3,
    fontSize: 12, fontFace: "Arial", color: C.gray, valign: "middle", margin: 0
  });

  // Write file
  const path = require("path");
  const outPath = path.join(__dirname, "KPBridge_Promo.pptx");
  await pres.writeFile({ fileName: outPath });
  console.log("PPT created:", outPath);
}

createPresentation().catch(console.error);
